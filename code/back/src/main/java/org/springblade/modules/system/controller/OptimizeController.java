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
package org.springblade.modules.system.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import lombok.AllArgsConstructor;
import jakarta.validation.Valid;

import org.springblade.common.cache.ParamCache;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.system.entity.OptimizeEntity;
import org.springblade.modules.system.excel.OptimizeExcel;
import org.springblade.modules.system.vo.OptimizeVO;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.modules.system.wrapper.OptimizeWrapper;
import org.springblade.modules.system.service.IOptimizeService;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.tool.constant.BladeConstant;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 优化内容 控制器
 *
 * @author BladeX
 * @since 2024-08-04
 */
@RestController
@AllArgsConstructor
@RequestMapping("blade-system/optimize")
@Tag(name = "优化内容接口", description = "优化内容")
public class OptimizeController extends BladeController {

	private final IOptimizeService optimizeService;

	/**
	 * 优化内容 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入optimize")
	public R<OptimizeVO> detail(OptimizeEntity optimize) {
		OptimizeEntity detail = optimizeService.getOne(Condition.getQueryWrapper(optimize));
		return R.data(OptimizeWrapper.build().entityVO(detail));
	}
	/**
	 * 优化内容 分页
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入optimize")
	public R<IPage<OptimizeVO>> list(@RequestParam Map<String, Object> optimize, Query query) {
		IPage<OptimizeEntity> pages = optimizeService.page(Condition.getPage(query), Condition.getQueryWrapper(optimize, OptimizeEntity.class));
		return R.data(OptimizeWrapper.build().pageVO(pages));
	}

	/**
	 * 优化内容 自定义分页
	 */
	@PostMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入optimize")
	public R<IPage<OptimizeVO>> page(OptimizeVO optimize, Query query) {
		IPage<OptimizeVO> pages = optimizeService.selectOptimizePage(Condition.getPage(query), optimize);
		return R.data(pages);
	}

	/**
	 * 优化内容 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入optimize")
	public R save(@Valid @RequestBody OptimizeEntity optimize) {
		return R.status(optimizeService.save(optimize));
	}

	/**
	 * 优化内容 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入optimize")
	public R update(@Valid @RequestBody OptimizeEntity optimize) {
		return R.status(optimizeService.updateById(optimize));
	}

	/**
	 * 优化内容 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入optimize")
	public R submit(@Valid @RequestBody OptimizeEntity optimize) {
		optimize.setSystemCode(ParamCache.getValue("system.id"));
		return R.status(optimizeService.saveOrUpdate(optimize));
	}

	/**
	 * 优化内容 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(optimizeService.deleteLogic(Func.toLongList(ids)));
	}


	/**
	 * 导出数据
	 */
	@PostMapping("/export-optimize")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "导出数据", description = "传入optimize")
	public void exportOptimize(@RequestParam Map<String, Object> optimize, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<OptimizeEntity> queryWrapper = Condition.getQueryWrapper(optimize, OptimizeEntity.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(Optimize::getTenantId, bladeUser.getTenantId());
		//}
		queryWrapper.lambda().eq(OptimizeEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<OptimizeExcel> list = optimizeService.exportOptimize(queryWrapper);
		ExcelUtil.export(response, "优化内容数据" + DateUtil.time(), "优化内容数据表", list, OptimizeExcel.class);
	}

}
