package org.springblade.modules.quartz.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import org.springblade.common.constant.CommonConstant;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.modules.desk.enums.BizScheduledTriggerModeEnum;
import org.springblade.modules.quartz.enums.BizTaskEnum;
import org.springblade.modules.quartz.executor.*;
import org.springblade.modules.quartz.domain.SysJob;
import org.springblade.modules.quartz.service.ISysJobService;
import org.springblade.modules.quartz.template.TaskBuilder;
import org.springblade.modules.system.entity.InteractiveEntity;
import org.springblade.modules.system.service.IInteractiveService;
import org.springframework.stereotype.Component;

@Component("SyncDataTask")
@AllArgsConstructor
public class SyncDataTask {

	private final SyncPeopleExecutor syncPeopleTaskExecutor;

	private final SyncAddressExecutor syncAddressTaskExecutor;

	private final SyncDemoExecutor demoTaskExecutor;

	private final ISysJobService jobService;

	private final IInteractiveService iInteractiveService;


	private final PostAttachToFilesputExecutor postAttachToFilesputExecutor;

	private final SyncFilesStoreExecutor filesStoreExecutor;

	private final BaseDictExecutor baseDictExecutor;

	private final SyncPdfSignExecutor pdfSignExecutor;

	private final TaskBuilder taskBuilder;

	public void syncSgnPdf() {

		System.out.println("开始执行syncPdfSign");
		this.checkExternal(BizTaskEnum.TASK_SYNC_SIGNPDF.getCode());
		pdfSignExecutor.doSyncPdfSignDataByInterface(BizScheduledTriggerModeEnum.TASK_SYNC_AUTO.getType());
	}

	public void syncAddress() {
		System.out.println("开始执行syncAddress");
		this.checkExternal(BizTaskEnum.TASK_SYNC_ADDRESS.getQuartzTaskName());
		syncAddressTaskExecutor.doSyncAddressDataByInterface(BizScheduledTriggerModeEnum.TASK_SYNC_AUTO.getType());
	}

	public void syncPeople() {
		System.out.println("开始执行syncPeople");
		this.checkExternal(BizTaskEnum.TASK_SYNC_PEOPLE.getQuartzTaskName());
		syncPeopleTaskExecutor.doSyncPeopleDataByInterface(BizScheduledTriggerModeEnum.TASK_SYNC_AUTO.getType());
	}

	public void postAttach() {
		System.out.println("开始执行postAttach");
		this.checkExternal(BizTaskEnum.TASK_POST_ATTACH.getQuartzTaskName());
		postAttachToFilesputExecutor.doPostAttachDataByInterface(BizScheduledTriggerModeEnum.TASK_SYNC_AUTO.getType());
	}

	public void syncFiles() {
		System.out.println("开始执行syncFiles");
		this.checkExternal(BizTaskEnum.TASK_SYNC_FILES.getQuartzTaskName());
		filesStoreExecutor.doSyncFilesDataByInterface(BizScheduledTriggerModeEnum.TASK_SYNC_AUTO.getType());
	}

	public void syncBaseDict() {
		System.out.println("开始执行syncBaseDict");
		this.checkExternal(BizTaskEnum.TASK_SYNC_BASE_DICT.getQuartzTaskName());
		baseDictExecutor.doSyncBizParamByInterface(BizScheduledTriggerModeEnum.TASK_SYNC_AUTO.getType());
	}


	public void syncByTemplate(String id) {
		SysJob job = jobService.getById(id);
		System.out.println("开始执行syncByTemplate");
		this.checkExternal(job);
		taskBuilder.doSyncByTem(job.getJobRunTemplate());
	}




	public void demoTask()
	{
		this.checkExternal(BizTaskEnum.TASK_SYNC_TEST.getQuartzTaskName());
		demoTaskExecutor.doSyncDemoDataByInterface(BizScheduledTriggerModeEnum.TASK_SYNC_AUTO.getType());
		System.out.println("执行无参方法");
	}

	//执行前需要检验《对外交互》功能是否打开当前同步
	private void checkExternal(String taskType) {
		SysJob sysJob = jobService.getOne(new LambdaQueryWrapper<SysJob>()
			.eq(SysJob::getInvokeTarget,taskType).last(" limit 1 "));
		if (sysJob!=null&&CommonConstant.YES == sysJob.getIsExternal()){
			if (sysJob.getInteractionId()!=null) {
				InteractiveEntity interactiveEntity = iInteractiveService.getById(sysJob.getInteractionId());
				if (interactiveEntity==null){
					throw new ServiceException("没有配置对外交互信息");
				} else if (interactiveEntity.getStatus() != CommonConstant.DB_STATUS_NORMAL){
					throw new ServiceException(interactiveEntity.getInteractiveName()+"功能已经在【对外交互】功能内关闭，请打开后重试！");
				}
			}else {
				throw new ServiceException("没有配置对外交互信息");
			}
		}
	}

	private void checkExternal(SysJob sysJob) {
		if (sysJob!=null&&CommonConstant.YES == sysJob.getIsExternal()){
			if (sysJob.getInteractionId()!=null) {
				InteractiveEntity interactiveEntity = iInteractiveService.getById(sysJob.getInteractionId());
				if (interactiveEntity==null){
					throw new ServiceException("没有配置对外交互信息");
				} else if (interactiveEntity.getStatus() != CommonConstant.DB_STATUS_NORMAL){
					throw new ServiceException(interactiveEntity.getInteractiveName()+"功能已经在【对外交互】功能内关闭，请打开后重试！");
				}
			}else {
				throw new ServiceException("没有配置对外交互信息");
			}
		}
	}
}
