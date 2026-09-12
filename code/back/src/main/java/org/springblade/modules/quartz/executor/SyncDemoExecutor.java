package org.springblade.modules.quartz.executor;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import org.springblade.common.constant.CommonConstant;
import org.springblade.common.rpc.apiplatform.constant.ApiPlatformUrlConstant;
import org.springblade.common.rpc.apiplatform.util.ApiPlatformHttp;
import org.springblade.common.rpc.apiplatform.util.ApiPlatformUtils;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.modules.desk.entity.ScheduledLogEntity;
import org.springblade.modules.quartz.enums.BizTaskEnum;
import org.springblade.modules.desk.service.IScheduledLogService;
import org.springblade.modules.desk.vo.AddressSyncVO;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@AllArgsConstructor
public class SyncDemoExecutor {
	private final IScheduledLogService scheduledLogService;




	public void doSyncDemoDataByInterface(String triggerMode) {

		ScheduledLogEntity log = new ScheduledLogEntity(){{
			setExecTime(new Date());
			setTaskCode(BizTaskEnum.TASK_SYNC_TEST.getCode());
			setTaskName(BizTaskEnum.TASK_SYNC_TEST.getName());
			setTriggerMode(triggerMode);
		}};
		//List<AddressSyncVO> addressSyncVOList;
		Map params;
		int syncSize=0;
		try {


			ScheduledLogEntity scheduledLog = scheduledLogService.getOne(
				new LambdaQueryWrapper<ScheduledLogEntity>()
					.eq(ScheduledLogEntity::getTaskCode, BizTaskEnum.TASK_SYNC_ADDRESS.getCode())
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
			// TODO 编写同步数据、记录日志

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


	private List<AddressSyncVO> getAddressData(Map<String,Object> param) {
		JsonNode res = ApiPlatformHttp.commonGetStr(param, ApiPlatformUrlConstant.SYNC_ADDRESS_URL);
		//解析返回结果
		return JSON.parseArray(ApiPlatformUtils.getDataFromJsonNode(res),AddressSyncVO.class);
	}
}
