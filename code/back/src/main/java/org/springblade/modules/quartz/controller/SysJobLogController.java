package org.springblade.modules.quartz.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.quartz.domain.SysJob;
import org.springblade.modules.quartz.domain.SysJobLog;
import org.springblade.modules.quartz.service.ISysJobLogService;
import org.springblade.modules.quartz.service.ISysJobService;
import org.springblade.modules.system.entity.ApiScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import java.util.Map;

/**
 * 调度日志操作处理
 *
 * @author pc
 */
@RestController
@RequestMapping("/blade-job/jobLog")
public class SysJobLogController  extends BladeController
{


    @Autowired
    private ISysJobLogService jobLogService;




    @PostMapping("/list")
    @ResponseBody
    public R<IPage<SysJobLog>> list(@RequestParam Map<String, Object> dataScope, Query query)
    {
		LambdaQueryWrapper<SysJobLog> lambdaQueryWrapper = Condition.getQueryWrapper(dataScope, SysJobLog.class).lambda();
		lambdaQueryWrapper.orderByDesc(SysJobLog::getStartTime);
		IPage<SysJobLog> pages = jobLogService.page(
			Condition.getPage(query),lambdaQueryWrapper);

		return R.data(pages);
    }


	/**
	 * 定时任务日志表 详情
	 */
	@PostMapping("/detail")
	public R<SysJobLog> detail(SysJobLog jobLog) {
		SysJobLog detail = jobLogService.getOne(Condition.getQueryWrapper(jobLog));
		return R.data(detail);
	}



    @PostMapping("/remove")
    @ResponseBody
    public R remove(String ids)
    {
        return R.status(jobLogService.deleteJobLogByIds(ids));
    }



    @PostMapping("/clean")
    @ResponseBody
    public R clean()
    {
        jobLogService.cleanJobLog();
        return R.success("操作成功!");
    }
}
