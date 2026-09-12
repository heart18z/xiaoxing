package org.springblade.modules.quartz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.springblade.common.constant.CommonConstant;
import org.springblade.common.utils.CommonUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.quartz.constants.ScheduleConstants;
import org.springblade.modules.quartz.domain.SysJob;
import org.springblade.modules.quartz.domain.SysJobLog;
import org.springblade.modules.quartz.enums.BizTaskEnum;
import org.springblade.modules.quartz.exception.TaskException;
import org.springblade.modules.quartz.mapper.SysJobLogMapper;
import org.springblade.modules.quartz.mapper.SysJobMapper;
import org.springblade.modules.quartz.service.ISysJobLogService;
import org.springblade.modules.quartz.service.ISysJobService;
import org.springblade.modules.quartz.util.CronUtils;
import org.springblade.modules.quartz.util.ScheduleUtils;
import org.springblade.modules.system.entity.InteractiveEntity;
import org.springblade.modules.system.service.IInteractiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 定时任务调度信息 服务层
 *
 * @author pc
 */
@Service
@RequiredArgsConstructor(onConstructor_= {@Lazy})
public class SysJobServiceImpl extends BaseServiceImpl<SysJobMapper, SysJob> implements ISysJobService
{

    private final Scheduler scheduler;

    private final SysJobMapper jobMapper;

	private final IInteractiveService iInteractiveService;

    /**
     * 项目启动时，初始化定时器
     * 主要是防止手动修改数据库导致未同步到定时任务处理（注：不能手动修改数据库ID和任务组名，否则会导致脏数据）
     */
    @PostConstruct
    public void init() throws SchedulerException, TaskException
    {
        scheduler.clear();
        List<SysJob> jobList = this.list();
        for (SysJob job : jobList)
        {
            ScheduleUtils.createScheduleJob(scheduler, job);
        }
    }

    /**
     * 获取quartz调度器的计划任务列表
     *
     * @param job 调度信息
     * @return
     */
    @Override
    public List<SysJob> selectJobList(SysJob job)
    {
        return jobMapper.selectJobList(job);
    }

    /**
     * 通过调度任务ID查询调度信息
     *
     * @param jobId 调度任务ID
     * @return 调度任务对象信息
     */
    @Override
    public SysJob selectJobById(Long jobId)
    {
        return this.getById(jobId);
    }

    /**
     * 暂停任务
     *
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean pauseJob(SysJob job) throws SchedulerException
    {
        Long jobId = job.getId();
        String jobGroup = job.getJobGroup();
        job.setProcessStatus(ScheduleConstants.Status.PAUSE.getValue());
        boolean updated = this.updateById(job);
        if (updated)
        {
            scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return updated;
    }

    /**
     * 恢复任务
     *
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean resumeJob(SysJob job) throws SchedulerException
    {
        Long jobId = job.getId();
        String jobGroup = job.getJobGroup();
        job.setProcessStatus(ScheduleConstants.Status.NORMAL.getValue());
        boolean rows = this.updateById(job);
        if (rows)
        {
            scheduler.resumeJob(ScheduleUtils.getJobKey(jobId, jobGroup));;
        }
        return rows;
    }

    /**
     * 删除任务后，所对应的trigger也将被删除
     *
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteJob(SysJob job) throws SchedulerException
    {
        Long jobId = job.getId();
        String jobGroup = job.getJobGroup();
        boolean rows = this.removeById(jobId);
        if (rows )
        {
            scheduler.deleteJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return rows;
    }

    /**
     * 批量删除调度信息
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteJobByIds(String ids) throws SchedulerException
    {
        Long[] jobIds = Func.toLongArray(ids);
        for (Long jobId : jobIds)
        {
            SysJob job = this.getById(jobId);
            deleteJob(job);
        }
    }

    /**
     * 任务调度状态修改
     *
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean changeStatus(SysJob job) throws SchedulerException
    {
        boolean rows = false;
        String status = job.getProcessStatus();
        if (ScheduleConstants.Status.NORMAL.getValue().equals(status))
        {
			SysJob dbJob = this.getById(job.getId());
			if (dbJob!=null&&dbJob.getInteractionId()!=null) {
				InteractiveEntity interactiveEntity = iInteractiveService.getById(dbJob.getInteractionId());
				if (interactiveEntity!=null&&interactiveEntity.getStatus() != CommonConstant.DB_STATUS_NORMAL) {
					throw new ServiceException("启用失败！请先在对外交互中启用【"+interactiveEntity.getInteractiveName()+"】");
				}else if (interactiveEntity==null) {
					throw new ServiceException("启用失败！对外交互不存在，请重新关联");

				}
			}

            rows = resumeJob(job);
        }
        else if (ScheduleConstants.Status.PAUSE.getValue().equals(status))
        {
            rows = pauseJob(job);
        }
        return rows;
    }

    /**
     * 立即运行任务
     *
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean run(SysJob job) throws SchedulerException
    {
        boolean result = false;
        Long jobId = job.getId();
        SysJob tmpObj = getById(job.getId());
        // 参数
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(ScheduleConstants.TASK_PROPERTIES, tmpObj);
        JobKey jobKey = ScheduleUtils.getJobKey(jobId, tmpObj.getJobGroup());
        if (scheduler.checkExists(jobKey))
        {
            result = true;
            scheduler.triggerJob(jobKey, dataMap);
        }
        return result;
    }

    /**
     * 新增任务
     *
     * @param job 调度信息 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertJob(SysJob job) throws SchedulerException, TaskException
    {
		long targetOnly = this.count(new LambdaQueryWrapper<SysJob>()
			.eq(SysJob::getInvokeTarget,job.getInvokeTarget()));
//		if (targetOnly>0) {
//			throw new ServiceException("调用目标已存在，不能重复添加！");
//		}

		job.setJobCode(buildUniqueOrgCode());
        job.setProcessStatus(ScheduleConstants.Status.PAUSE.getValue());
        boolean saved = this.save(job);
        if (saved)
        {
            ScheduleUtils.createScheduleJob(scheduler, job);
        }
        return saved;
    }

    /**
     * 更新任务的时间表达式
     *
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateJob(SysJob job) throws SchedulerException, TaskException
    {
		long targetOnly = this.count(new LambdaQueryWrapper<SysJob>()
			.eq(SysJob::getInvokeTarget,job.getInvokeTarget())
			.ne(SysJob::getId,job.getId()));
//		if (targetOnly>0) {
//			throw new ServiceException("调用目标已存在，不能重复添加！");
//		}

        SysJob properties = getById(job.getId());
		if (Func.isBlank(properties.getJobCode()) && Func.isBlank(job.getJobCode())) {
			job.setJobCode(buildUniqueOrgCode());
		}

        boolean rows = this.updateById(job);
        if (rows)
        {
            updateSchedulerJob(job, properties.getJobGroup());
        }
        return rows;
    }

    /**
     * 更新任务
     *
     * @param job 任务对象
     * @param jobGroup 任务组名
     */
    public void updateSchedulerJob(SysJob job, String jobGroup) throws SchedulerException, TaskException
    {
        Long jobId = job.getId();
        // 判断是否存在
        JobKey jobKey = ScheduleUtils.getJobKey(jobId, jobGroup);
        if (scheduler.checkExists(jobKey))
        {
            // 防止创建时存在数据问题 先移除，然后在执行创建操作
            scheduler.deleteJob(jobKey);
        }
        ScheduleUtils.createScheduleJob(scheduler, job);
    }

    /**
     * 校验cron表达式是否有效
     *
     * @param cronExpression 表达式
     * @return 结果
     */
    @Override
    public boolean checkCronExpressionIsValid(String cronExpression)
    {
        return CronUtils.isValid(cronExpression);
    }

	@Override
	public boolean cleanData(SysJob job) {
		if (Func.isBlank(job.getInvokeTarget())) {
			throw new ServiceException("清除数据失败！目标未配置");
		}

		Map<String,String> enumsMap = Arrays.stream(BizTaskEnum.values())
			.filter(i->Func.isNotBlank(i.getQuartzTaskName())&&Func.isNotBlank(i.getTableName()))
			.collect(Collectors.toMap(BizTaskEnum::getQuartzTaskName,BizTaskEnum::getTableName,(v1,v2)->v1));

		String tableName = enumsMap.get(job.getInvokeTarget());
		if (Func.isNotBlank(tableName))	 {
			this.baseMapper.cleanTable(tableName);
			return true;
		}else {
			throw new ServiceException("清除数据失败！该数据同步配置不能清空表！");
		}
	}

	private String  buildUniqueOrgCode() {
		String value = CommonUtil.createUniqueCode(6);
		long count = this.count(new LambdaQueryWrapper<SysJob>()
			.eq(SysJob::getJobCode,value));
		if (count >0) {
			value = buildUniqueOrgCode();
		}
		return value;
	}



}
