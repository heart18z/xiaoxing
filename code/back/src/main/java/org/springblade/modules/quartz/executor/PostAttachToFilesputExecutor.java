package org.springblade.modules.quartz.executor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import org.springblade.common.cache.ParamCache;
import org.springblade.modules.resource.entity.AttachSourceEntity;
import org.springblade.modules.resource.service.IAttachSourceService;
import org.springblade.common.constant.CommonConstant;
import org.springblade.common.rpc.apiplatform.constant.ApiPlatformUrlConstant;
import org.springblade.common.rpc.apiplatform.util.ApiPlatformHttp;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.desk.entity.ScheduledLogEntity;
import org.springblade.modules.desk.service.IScheduledLogService;
import org.springblade.modules.quartz.enums.BizTaskEnum;
import org.springblade.modules.quartz.support.FileItem;
import org.springblade.modules.quartz.support.FilesSyncVO;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.service.IUserService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class PostAttachToFilesputExecutor {

	private final IScheduledLogService scheduledLogService;

	private final IAttachSourceService attachSourceService;

	private final IUserService userService;
	@Transactional
	public void doPostAttachDataByInterface(String triggerMode) {

		ScheduledLogEntity log = new ScheduledLogEntity(){{
			setExecTime(new Date());
			setTaskCode(BizTaskEnum.TASK_POST_ATTACH.getCode());
			setTaskName(BizTaskEnum.TASK_POST_ATTACH.getName());
			setTriggerMode(triggerMode);
		}};
		List<FileItem> fileItemList;
		int syncSize=0;
		try {
			ScheduledLogEntity scheduledLog = scheduledLogService.getOne(
				new LambdaQueryWrapper<ScheduledLogEntity>()
					.eq(ScheduledLogEntity::getTaskCode, BizTaskEnum.TASK_POST_ATTACH.getCode())
					.eq(ScheduledLogEntity::getSuccess, CommonConstant.YES)
					.orderByDesc(ScheduledLogEntity::getExecTime)
					.last(" limit 1")
			);
			LambdaQueryWrapper<AttachSourceEntity> lambdaQueryWrapper = new LambdaQueryWrapper<AttachSourceEntity>()
				.eq(AttachSourceEntity::getIsPhysicalSave,CommonConstant.YES);

			if (scheduledLog != null) {
				//创建后只同步一次，因为后面都不能再编辑数据
				lambdaQueryWrapper.ge(AttachSourceEntity::getCreateTime,scheduledLog.getExecTime());
			}
			//获取需要同步的数据
			List<AttachSourceEntity> sourceEntities = attachSourceService.list(lambdaQueryWrapper);
			List<AttachSourceEntity> sourceUpdateEntities = sourceEntities.stream().filter(
				i->Func.isBlank(i.getSafekeepStatus())).peek(i->i.setSafekeepStatus("keeping")
			).collect(Collectors.toList());

			Map<Long,String> userIdNameMap= userService.list().stream().filter(i-> Func.isNotBlank(i.getName()))
				.collect(Collectors.toMap(User::getId,User::getRealName,(v1, v2)->v1));

			if (!sourceEntities.isEmpty()) {
				fileItemList = sourceEntities.stream().map(i->{
					FileItem fileItem=new FileItem();
					fileItem.setFileName(i.getDiyFileName());
					fileItem.setUrl(i.getLink());
					fileItem.setFileSourceNum(i.getFileNum());
					fileItem.setPropertyCode(i.getFilePhysicalPosition());
					fileItem.setUploadTime(i.getUpdateTime());
					fileItem.setSourceId(i.getId());
					fileItem.setUploadUserName(userIdNameMap.get(i.getUpdateUser()));
					return fileItem;
				}).collect(Collectors.toList());
				FilesSyncVO syncVO = new FilesSyncVO(){{
					setFileList(fileItemList);
					setSystemId(ParamCache.getValue("system.id"));
				}};
				syncSize= fileItemList.size();

				R<Object> objectR =  ApiPlatformHttp.commonPostByInteractive(syncVO,
					ApiPlatformUrlConstant.POST_ATTACH_INTERACTIVE_CODE,
					ApiPlatformUrlConstant.POST_ATTACH_URL);
				if (objectR ==null||objectR.getCode()!=200) {
					throw new ServiceException("数据发送失败"+objectR!=null?objectR.getMsg():"null");
				}
				if (!sourceUpdateEntities.isEmpty()) {
					attachSourceService.updateBatchById(sourceUpdateEntities);
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


}
