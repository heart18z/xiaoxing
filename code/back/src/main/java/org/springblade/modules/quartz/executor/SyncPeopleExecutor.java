package org.springblade.modules.quartz.executor;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import org.springblade.common.constant.CommonConstant;
import org.springblade.common.rpc.apiplatform.constant.ApiPlatformUrlConstant;
import org.springblade.core.http.HttpRequest;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.desk.entity.ScheduledLogEntity;
import org.springblade.modules.quartz.enums.BizTaskEnum;
import org.springblade.modules.desk.service.IScheduledLogService;
import org.springblade.modules.desk.vo.SyncPeopleVO;
import org.springblade.modules.quartz.constants.ScheduleConstants;
import org.springblade.modules.system.entity.InteractiveEntity;
import org.springblade.modules.system.entity.PeopleEntity;
import org.springblade.modules.system.service.IInteractiveService;
import org.springblade.modules.system.service.IPeopleService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class SyncPeopleExecutor {

	private final IScheduledLogService scheduledLogService;

	private final IPeopleService peopleService;

	private final IInteractiveService interactiveService;

	private final boolean isSyncPeopleIDNO = false;
	@Transactional
	public void doSyncPeopleDataByInterface(String triggerMode) {
		ScheduledLogEntity log = new ScheduledLogEntity(){{
			setExecTime(new Date());
			setTaskCode(BizTaskEnum.TASK_SYNC_PEOPLE.getCode());
			setTaskName(BizTaskEnum.TASK_SYNC_PEOPLE.getName());
			setTriggerMode(triggerMode);
		}};
		List<SyncPeopleVO> peopleVOS;
		Map params;
		int syncSize=0;
		try {

			ScheduledLogEntity scheduledLog = scheduledLogService.getOne(
				new LambdaQueryWrapper<ScheduledLogEntity>()
					.eq(ScheduledLogEntity::getTaskCode, BizTaskEnum.TASK_SYNC_PEOPLE.getCode())
					.eq(ScheduledLogEntity::getSuccess, CommonConstant.YES)
					.orderByDesc(ScheduledLogEntity::getExecTime)
					.last(" limit 1")

			);
			if (scheduledLog == null) {
				params = new HashMap<String, Object>() {{
					put("sfsc", 0);
				}};
			} else {
				params = new HashMap<String, Object>() {{
					put("statDate",
						DateUtil.format(scheduledLog.getExecTime(), DateUtil.PATTERN_DATE));
				}};
			}
			peopleVOS = getUserData(params);
			if (!peopleVOS.isEmpty()) {
				Map<String, Long> usersAccountMap = peopleService.list(new LambdaQueryWrapper<PeopleEntity>()
						.in(PeopleEntity::getAccount, peopleVOS.stream().map(SyncPeopleVO::getRyh).collect(Collectors.toList())))
					.stream().collect(Collectors.toMap(PeopleEntity::getAccount, PeopleEntity::getId, (v1, v2) -> v1));
				List<PeopleEntity> saveData = new ArrayList<>();
				for (SyncPeopleVO vo : peopleVOS) {
					PeopleEntity peopleEntity = new PeopleEntity() {{
						Long id =usersAccountMap.get(vo.getRyh());
						setId(id);
						setAccount(vo.getRyh());
						setIsSync(ScheduleConstants.DATA_SOURCE_SYNC);
						setRealName(vo.getXm());
						if ("1".equals(vo.getXb()) ) {
							setSex(1);
						} else if ("2".equals(vo.getXb())) {
							setSex(2);
						}
						setEmail(vo.getDzyj());
						setWechat(vo.getWxid());
						setPhone(vo.getSj());
						if (Func.isNotBlank(vo.getCsrq())) {
							setBirthday(DateUtil.parse(vo.getCsrq(), DateUtil.PATTERN_DATE));
						}
						if (isSyncPeopleIDNO) {
							setIdNo(vo.getSfzjh());
						}

					}};
					saveData.add(peopleEntity);
				}
				if (!saveData.isEmpty()) {
					syncSize =saveData.size();
					peopleService.saveOrUpdateBatch(saveData);

				}
			}
			log.setUpdateRows(syncSize);
			log.setTaskContent("共执行"+syncSize+"条数据！");
			log.setSuccess(CommonConstant.YES);

		}catch (Exception e) {
			log.setTaskContent("数据同步任务执行失败！"+e);
			log.setSuccess(CommonConstant.NO);
			throw new ServiceException("数据同步任务执行失败！"+e.getMessage());

		}finally {
			log.setEndTime(new Date());
			scheduledLogService.saveData(log);
		}

	}

	private List<SyncPeopleVO> getUserData(Map<String,Object> param) {
		String url = ApiPlatformUrlConstant.SYNC_PEOPLE_FULL_URL;
		String interactiveCode = ApiPlatformUrlConstant.SYNC_PEOPLE_INTERACTIVE_CODE;
		if (Func.isNotBlank(interactiveCode)) {
			InteractiveEntity interactiveEntity = interactiveService.getOne(
				new LambdaQueryWrapper<InteractiveEntity>()
					.select(InteractiveEntity::getAddress)
					.eq(InteractiveEntity::getInteractiveCode,interactiveCode)
					.last(" limit 1")
			);
			if (interactiveEntity!=null&&Func.isNotBlank(interactiveEntity.getAddress())
				&& !interactiveEntity.getAddress().startsWith("_")
			) {
				url = interactiveEntity.getAddress();
			}
		}

		JsonNode res = HttpRequest.get(url)
			.queryMap(param)
			.execute()
			.onSuccess(responseSpec -> responseSpec.asJsonNode());
		//简单解析返回结果
		String data = res.get("data").toString();
		return JSON.parseArray(data, SyncPeopleVO.class);
	}

}
