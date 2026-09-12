/*
 *      Copyright (c) 2018-2028, Chill Zhuang All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the dreamlu.net developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: Chill 庄骞 (smallchill@163.com)
 */
package org.springblade.modules.desk.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.desk.entity.ScheduledLogEntity;
import org.springblade.modules.desk.excel.ScheduledLogExcel;
import org.springblade.modules.desk.service.IScheduledLogService;
import org.springblade.modules.desk.vo.ScheduledLogVO;
import org.springblade.modules.desk.wrapper.ScheduledLogWrapper;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 定时任务日志表 控制器
 *
 * @author BladeX
 * @since 2024-08-05
 */
@RestController
@AllArgsConstructor
@RequestMapping("blade-scheduledLog/scheduledLog")
@Tag(name = "定时任务日志表接口", description = "定时任务日志表")
public class ScheduledLogController extends BladeController {

	private final IScheduledLogService scheduledLogService;

	/**
	 * 定时任务日志表 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入scheduledLog")
	public R<ScheduledLogVO> detail(ScheduledLogEntity scheduledLog) {
		ScheduledLogEntity detail = scheduledLogService.getOne(Condition.getQueryWrapper(scheduledLog));
		return R.data(ScheduledLogWrapper.build().entityVO(detail));
	}
	/**
	 * 定时任务日志表 分页
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入scheduledLog")
	public R<IPage<ScheduledLogVO>> list(@RequestParam Map<String, Object> scheduledLog, Query query) {
		IPage<ScheduledLogEntity> pages = scheduledLogService.page(Condition.getPage(query), Condition.getQueryWrapper(scheduledLog, ScheduledLogEntity.class));
		return R.data(ScheduledLogWrapper.build().pageVO(pages));
	}

	/**
	 * 定时任务日志表 自定义分页
	 */
	@PostMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入scheduledLog")
	public R<IPage<ScheduledLogVO>> page(ScheduledLogVO scheduledLog, Query query) {
		IPage<ScheduledLogVO> pages = scheduledLogService.selectScheduledLogPage(Condition.getPage(query), scheduledLog);
		return R.data(pages);
	}

	/**
	 * 定时任务日志表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入scheduledLog")
	public R save(@Valid @RequestBody ScheduledLogEntity scheduledLog) {
		return R.status(scheduledLogService.save(scheduledLog));
	}

	/**
	 * 定时任务日志表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入scheduledLog")
	public R update(@Valid @RequestBody ScheduledLogEntity scheduledLog) {
		return R.status(scheduledLogService.updateById(scheduledLog));
	}

	/**
	 * 定时任务日志表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入scheduledLog")
	public R submit(@Valid @RequestBody ScheduledLogEntity scheduledLog) {
		return R.status(scheduledLogService.saveOrUpdate(scheduledLog));
	}

	/**
	 * 定时任务日志表 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(scheduledLogService.deleteLogic(Func.toLongList(ids)));
	}


	/**
	 * 导出数据
	 */
	@PostMapping("/export-scheduledLog")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "导出数据", description = "传入scheduledLog")
	public void exportScheduledLog(@RequestParam Map<String, Object> scheduledLog, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<ScheduledLogEntity> queryWrapper = Condition.getQueryWrapper(scheduledLog, ScheduledLogEntity.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(ScheduledLog::getTenantId, bladeUser.getTenantId());
		//}
		queryWrapper.lambda().eq(ScheduledLogEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<ScheduledLogExcel> list = scheduledLogService.exportScheduledLog(queryWrapper);
		ExcelUtil.export(response, "定时任务日志表数据" + DateUtil.time(), "定时任务日志表数据表", list, ScheduledLogExcel.class);
	}

}
