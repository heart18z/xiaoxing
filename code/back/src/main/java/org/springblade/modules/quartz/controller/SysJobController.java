package org.springblade.modules.quartz.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springblade.common.constant.CommonConstant;
import org.springblade.common.utils.CommonUtil;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.quartz.constants.ScheduleConstants;
import org.springblade.modules.quartz.domain.SysJob;
import org.springblade.modules.quartz.domain.SysJobSchedule;
import org.springblade.modules.quartz.domain.SysJobVO;
import org.springblade.modules.quartz.enums.BizTaskEnum;
import org.springblade.modules.quartz.exception.TaskException;
import org.springblade.modules.quartz.service.ISysJobService;
import org.springblade.modules.quartz.util.CronUtils;
import org.springblade.modules.quartz.util.ScheduleUtils;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 调度任务信息操作处理
 *
 * @author pc
 */
@RestController
@RequestMapping("/blade-job/job")
public class SysJobController  extends BladeController
{

    @Autowired
    private ISysJobService jobService;



	@PostMapping("/list")
	@ResponseBody
	public R<IPage<SysJobVO>> list(@RequestParam Map<String, Object> dataScope, Query query)
	{


		Map<String,String> enumsMap = Arrays.stream(BizTaskEnum.values())
			.filter(i->Func.isNotBlank(i.getQuartzTaskName())&&Func.isNotBlank(i.getTableName()))
			.collect(Collectors.toMap(BizTaskEnum::getQuartzTaskName,BizTaskEnum::getTableName,(v1,v2)->v1));


		IPage<SysJob> pages = jobService.page(
			Condition.getPage(query), Condition.getQueryWrapper(dataScope, SysJob.class));
		List<SysJobVO> sysJobVOS = pages.getRecords().stream().map(i->{
			SysJobVO vo = BeanUtil.copy(i, SysJobVO.class);
			String tableName = enumsMap.get(vo.getInvokeTarget());
			vo.setTableName(tableName);
			return vo;
		}).collect(Collectors.toList());

		IPage<SysJobVO> res= new Page(pages.getCurrent(), pages.getSize(), pages.getTotal());
		res.setRecords(sysJobVOS);
		return R.data(res);
	}


	/**
	 * 定时任务 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入job")
	public R<SysJob> detail(SysJob job) {
		SysJob detail = jobService.getOne(Condition.getQueryWrapper(job));
		return R.data(detail);
	}




    @PostMapping("/remove")
    @ResponseBody
    public R remove(String ids) throws SchedulerException
    {
        jobService.deleteJobByIds(ids);
        return R.success("操作成功");
    }


    /**
     * 任务调度状态修改
     */

    @PostMapping("/changeStatus")
    @ResponseBody
    public R changeStatus(@RequestBody SysJob job) throws SchedulerException
    {
        SysJob newJob = jobService.getById(job.getId());
        newJob.setProcessStatus(job.getProcessStatus());
        return R.status(jobService.changeStatus(newJob));
    }

    /**
     * 任务调度立即执行一次
     */

    @PostMapping("/run")
    @ResponseBody
    public R run(@RequestBody SysJob job) throws SchedulerException
    {
        boolean result = jobService.run(job);
        return result ? R.success("操作成功") : R.fail("任务不存在或已过期！");
    }


    /**
     * 新增保存调度
     */
    @PostMapping("/add")
    @ResponseBody
    public R addSave(@Validated @RequestBody SysJob job) throws SchedulerException, TaskException
    {
        if (!CronUtils.isValid(job.getCronExpression()))
        {
            return R.fail("新增任务'" + job.getJobName() + "'失败，Cron表达式不正确");
        }
        else if (StringUtils.containsIgnoreCase(job.getInvokeTarget(), ScheduleConstants.LOOKUP_RMI))
        {
            return R.fail("新增任务'" + job.getJobName() + "'失败，目标字符串不允许'rmi'调用");
        }
        else if (StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), new String[] { ScheduleConstants.LOOKUP_LDAP, ScheduleConstants.LOOKUP_LDAPS }))
        {
            return R.fail("新增任务'" + job.getJobName() + "'失败，目标字符串不允许'ldap(s)'调用");
        }
        else if (StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), new String[] { ScheduleConstants.HTTP, ScheduleConstants.HTTPS }))
        {
            return R.fail("新增任务'" + job.getJobName() + "'失败，目标字符串不允许'http(s)'调用");
        }
        else if (StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), ScheduleConstants.JOB_ERROR_STR))
        {
            return R.fail("新增任务'" + job.getJobName() + "'失败，目标字符串存在违规");
        }
        else if (!ScheduleUtils.whiteList(job.getInvokeTarget()))
        {
            return R.fail("新增任务'" + job.getJobName() + "'失败，目标字符串不在白名单内");
        }
        job.setCreateUser(AuthUtil.getUserId());
        return R.status(jobService.insertJob(job));
    }

    /**
     * 修改调度
     */

    /**
     * 修改保存调度
     */

    @PostMapping("/edit")
    @ResponseBody
    public R editSave(@Validated @RequestBody SysJob job) throws SchedulerException, TaskException
    {
        if (!CronUtils.isValid(job.getCronExpression()))
        {
            return R.fail("修改任务'" + job.getJobName() + "'失败，Cron表达式不正确");
        }
        else if (StringUtils.containsIgnoreCase(job.getInvokeTarget(), ScheduleConstants.LOOKUP_RMI))
        {
            return R.fail("修改任务'" + job.getJobName() + "'失败，目标字符串不允许'rmi'调用");
        }
        else if (StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), new String[] { ScheduleConstants.LOOKUP_LDAP, ScheduleConstants.LOOKUP_LDAPS }))
        {
            return R.fail("修改任务'" + job.getJobName() + "'失败，目标字符串不允许'ldap'调用");
        }
        else if (StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), new String[] { ScheduleConstants.HTTP, ScheduleConstants.HTTPS }))
        {
            return R.fail("修改任务'" + job.getJobName() + "'失败，目标字符串不允许'http(s)'调用");
        }
        else if (StringUtils.containsAnyIgnoreCase(job.getInvokeTarget(), ScheduleConstants.JOB_ERROR_STR))
        {
            return R.fail("修改任务'" + job.getJobName() + "'失败，目标字符串存在违规");
        }
        else if (!ScheduleUtils.whiteList(job.getInvokeTarget()))
        {
            return R.fail("修改任务'" + job.getJobName() + "'失败，目标字符串不在白名单内");
        }
        return R.status(jobService.updateJob(job));
    }

    /**
     * 校验cron表达式是否有效
     */
    @PostMapping("/checkCronExpressionIsValid")
    @ResponseBody
    public boolean checkCronExpressionIsValid(SysJob job)
    {
        return jobService.checkCronExpressionIsValid(job.getCronExpression());
    }



    /**
     * 查询cron表达式近5次的执行时间
     */
    @PostMapping("/queryCronExpression")
    @ResponseBody
    public R<List<SysJobSchedule>> queryCronExpression(@RequestParam(value = "ids", required = false) String ids)
    {

		if (Func.isBlank(ids)) {
			return R.data(Collections.emptyList());
		}

		List<SysJob> sysJobs = jobService.list(new LambdaQueryWrapper<SysJob>()
			.in(SysJob::getId,Func.toLongList(ids))
		);
		List<SysJobSchedule> jobSchedules = new ArrayList<>();
		for (SysJob job : sysJobs) {
			String cronExpression = job.getCronExpression();
			if (jobService.checkCronExpressionIsValid(cronExpression))
			{
				List<String> dateList = CronUtils.getRecentTriggerTime(cronExpression);
				jobSchedules.addAll(dateList.stream().map(m->
					new SysJobSchedule(){{
						setJobId(job.getId());
						setJobName(job.getJobName());
						setRunTime(m);
				}}).collect(Collectors.toList()));
			}
		}

		return R.data(jobSchedules);

    }

	/**
	 * 查询cron表达式近1次的执行时间
	 */
	@PostMapping("/queryCronExpressionByInvokeTarget")
	@ResponseBody
	public R<String> queryCronExpressionByInvokeTarget
	(@RequestParam(value = "invokeTarget", required = false) String invokeTarget)
	{

		if (Func.isBlank(invokeTarget)) {
			return R.data("",null);
		}

		List<SysJob> sysJobs = jobService.list(new LambdaQueryWrapper<SysJob>()
			.eq(SysJob::getInvokeTarget,invokeTarget)
		);
		if (!sysJobs.isEmpty()) {
			List<SysJob> startJobs = sysJobs.stream().filter(i -> ScheduleConstants.Status.NORMAL.getValue().equals(i.getProcessStatus()) ).collect(Collectors.toList());
			if (!startJobs.isEmpty()) {
				List<String> dateList = CronUtils.getRecentTriggerTime(startJobs.get(0).getCronExpression());
				if (dateList!=null && !dateList.isEmpty()) {
					return R.data(dateList.get(0),startJobs.get(0).getJobName());
				}
			}  else {
				return R.data("",sysJobs.get(0).getJobName());
			}

		}

		return R.data("",null);

	}


	@PostMapping("/clean-data")
	@ResponseBody
	public boolean cleanData(@RequestBody SysJob job)
	{
		return jobService.cleanData(job);
	}







}
