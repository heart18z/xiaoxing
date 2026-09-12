package org.springblade.modules.quartz.executor;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import org.springblade.common.cache.ParamCache;
import org.springblade.common.constant.CommonConstant;
import org.springblade.common.rpc.apiplatform.constant.ApiPlatformUrlConstant;
import org.springblade.common.rpc.apiplatform.util.ApiPlatformHttp;
import org.springblade.common.rpc.apiplatform.util.ApiPlatformUtils;
import org.springblade.common.rpc.support.SignFileVO;
import org.springblade.core.http.HttpRequest;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.desk.entity.ScheduledLogEntity;
import org.springblade.modules.desk.service.IScheduledLogService;
import org.springblade.modules.desk.vo.AddressSyncVO;
import org.springblade.modules.desk.vo.SyncPeopleVO;
import org.springblade.modules.quartz.constants.ScheduleConstants;
import org.springblade.modules.quartz.enums.BizTaskEnum;
import org.springblade.modules.resource.entity.Attach;
import org.springblade.modules.resource.entity.AttachSourceEntity;
import org.springblade.modules.resource.service.IAttachService;
import org.springblade.modules.resource.service.IAttachSourceService;
import org.springblade.modules.system.entity.PeopleEntity;
import org.springblade.modules.system.service.IPeopleService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
@Component
@AllArgsConstructor
public class SyncPdfSignExecutor {
	private final IScheduledLogService scheduledLogService;

	private final IPeopleService peopleService;

	private final IAttachSourceService attachSourceService;

	private final IAttachService attachService;

	@Transactional
	public void doSyncPdfSignDataByInterface(String triggerMode) {
		ScheduledLogEntity log = new ScheduledLogEntity(){{
			setExecTime(new Date());
			setTaskCode(BizTaskEnum.TASK_SYNC_SIGNPDF.getCode());
			setTaskName(BizTaskEnum.TASK_SYNC_SIGNPDF.getName());
			setTriggerMode(triggerMode);
		}};
		List<SignFileVO> signFileVOS;
		Map params;
		int syncSize=0;
		try {
			String systemId = ParamCache.getValue("system.id");

			signFileVOS = getSignPdfData(new HashMap<String, Object>(){{
				put("sourceSystemId",systemId);}});
			syncSize =signFileVOS.size();
			if (!signFileVOS.isEmpty()) {
				List<AttachSourceEntity> attachSourceEntities = attachSourceService
					.listByIds(signFileVOS.stream()
					.map(SignFileVO::getOriginalId).collect(Collectors.toList()));

				attachSourceService.updateBatchById(
					attachSourceEntities.stream().map(i->{
						i.setSignStatus("signStatus_12");
						return i;
					}).collect(Collectors.toList())
				);

				Map<Long,AttachSourceEntity> OriginalMap = attachSourceEntities.stream().collect(Collectors.toMap(
					AttachSourceEntity::getId,i->i
				));
				Map<Long,String> signFileVOMap = signFileVOS.stream().collect(Collectors.toMap(
					SignFileVO::getId,SignFileVO::getSignStatus
				));

				if (attachSourceEntities.isEmpty()) {
					throw new ServiceException("原始文件不存在！");
				}

				List<AttachSourceEntity> updateData = attachSourceService.getBaseMapper()
					.selectBatchIds(signFileVOS.stream()
					.map(SignFileVO::getId)
						.collect(Collectors.toList())).stream().map(i->{
							i.setSignStatus(signFileVOMap.get(i.getId()));
							return i;
					}).collect(Collectors.toList());

				attachSourceService.updateBatchById(updateData);

				List<Long> updateDataIds = updateData.stream().map(AttachSourceEntity::getId).collect(Collectors.toList());

				List<Attach> attaches = attachService.listByIds(attachSourceEntities.stream()
					.map(AttachSourceEntity::getAttachId).collect(Collectors.toList()));
				Map<Long,Attach> attachMap = attaches.stream().collect(Collectors.toMap(Attach::getId,i->i));
				List<Attach> attachSaveList = new ArrayList<>();
				Date now = new Date()
;				List<AttachSourceEntity> saveData = signFileVOS.stream()
					.filter(i->!updateDataIds.contains(i.getId()))
					.map(i->{
						AttachSourceEntity entity = OriginalMap.get(i.getOriginalId());
						if (entity==null) {return null;}
						entity.setId(i.getId());
						entity.setLink(i.getLink());
						entity.setSignStatus(i.getSignStatus());
						entity.setDiyFileName(i.getFileName());
						entity.setOriginalId(i.getOriginalId());
						entity.setCreateTime(now);
						entity.setUpdateTime(now);
						entity.setSourceId(null);
						Attach attach = attachMap.get(entity.getAttachId());
						if (attach==null) {return null;}
						Long attachId = IdWorker.getId();
						attach.setId(attachId);
						attach.setName(i.getFileName());
						attach.setCreateTime(now);
						attach.setUpdateTime(now);
						attach.setLink(i.getLink());
						attachSaveList.add(attach);
						entity.setAttachId(attachId);
						return entity;
					}).filter(Objects::nonNull)
					.collect(Collectors.toList());

				if (!attachSaveList.isEmpty()) {
					attachService.saveBatch(attachSaveList);
				}
				if (!saveData.isEmpty()) {

					attachSourceService.saveBatch(saveData);
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

	private List<SignFileVO> getSignPdfData(Map<String,Object> param) {

		JsonNode res = ApiPlatformHttp.commonPostStrByInteractive(
			param,
			ApiPlatformUrlConstant.SIGN_LIST_INTERACTIVE_CODE,
			ApiPlatformUrlConstant.SIGN_LIST_URL);

		List<SignFileVO> list = JSON.parseArray(ApiPlatformUtils.getDataFromJsonNode(res), SignFileVO.class);
		//解析返回结果
		return list==null?Collections.emptyList():list;
	}

}
