package org.springblade.modules.quartz.executor;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import org.springblade.common.cache.ParamCache;
import org.springblade.modules.resource.entity.AttachSourceEntity;
import org.springblade.modules.resource.service.IAttachSourceService;
import org.springblade.common.constant.CommonConstant;
import org.springblade.common.rpc.apiplatform.constant.ApiPlatformUrlConstant;
import org.springblade.common.rpc.apiplatform.util.ApiPlatformHttp;
import org.springblade.common.rpc.apiplatform.util.ApiPlatformUtils;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.desk.entity.ScheduledLogEntity;
import org.springblade.modules.desk.service.IScheduledLogService;
import org.springblade.modules.quartz.enums.BizTaskEnum;
import org.springblade.modules.quartz.support.FilesStoreVO;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class SyncFilesStoreExecutor {

	private final IScheduledLogService scheduledLogService;

	private final IAttachSourceService attachSourceService;


	@Transactional
	public void doSyncFilesDataByInterface(String triggerMode) {

		ScheduledLogEntity log = new ScheduledLogEntity(){{
			setExecTime(new Date());
			setTaskCode(BizTaskEnum.TASK_SYNC_FILES.getCode());
			setTaskName(BizTaskEnum.TASK_SYNC_FILES.getName());
			setTriggerMode(triggerMode);
		}};
		List<FilesStoreVO> filesStoreVOS;
		Map<String,Object> params=new HashMap<String,Object>(){{
			put("systemId", ParamCache.getValue("system.id"));
		}};
		int syncSize=0;
		try {

			ScheduledLogEntity scheduledLog = scheduledLogService.getOne(
				new LambdaQueryWrapper<ScheduledLogEntity>()
					.eq(ScheduledLogEntity::getTaskCode, BizTaskEnum.TASK_SYNC_FILES.getCode())
					.eq(ScheduledLogEntity::getSuccess, CommonConstant.YES)
					.orderByDesc(ScheduledLogEntity::getExecTime)
					.last(" limit 1")

			);

			if (scheduledLog != null) {
				params.put("syncUpdateTime", DateUtil.format(scheduledLog.getExecTime(), DateUtil.PATTERN_DATETIME));
			}
			filesStoreVOS = getFilesData(params);
			if (!filesStoreVOS.isEmpty()) {

				List<AttachSourceEntity> saveData =filesStoreVOS.stream()
					.map(i-> {
						AttachSourceEntity attachSource = new AttachSourceEntity(){{
							setId(i.getSourceId());
							setArchiveNo(i.getStoreCode());
							setKeeper(i.getKeeperName());
							setPhysicalPositionInfo(i.getFullStoreLocation());
							setSafekeepStatus(i.getSafekeepStatus());

						}};

						return attachSource;
					}).collect(Collectors.toList());

				syncSize =saveData.size();
				attachSourceService.updateBatchById(saveData);


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


	private List<FilesStoreVO> getFilesData(Map<String,Object> param) {
		JsonNode res = ApiPlatformHttp.commonGetStr(param, ApiPlatformUrlConstant.SYNC_FILES_URL);
		//解析返回结果
		return JSON.parseArray(ApiPlatformUtils.getDataFromJsonNode(res),FilesStoreVO.class);
	}
}
