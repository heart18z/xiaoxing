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

import org.springblade.core.secure.BladeUser;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.desk.enums.BizScheduledTriggerModeEnum;
import org.springblade.modules.quartz.executor.SyncAddressExecutor;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.modules.system.entity.PropertyRegionEntity;
import org.springblade.modules.system.vo.PropertyRegionVO;
import org.springblade.modules.system.excel.PropertyRegionExcel;
import org.springblade.modules.system.wrapper.PropertyRegionWrapper;
import org.springblade.modules.system.service.IPropertyRegionService;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.tool.constant.BladeConstant;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 物业信息表 控制器
 *
 * @author BladeX
 * @since 2024-08-14
 */
@RestController
@AllArgsConstructor
@RequestMapping("blade-propertyRegion/propertyRegion")
@Tag(name = "物业信息表接口", description = "物业信息表")
public class PropertyRegionController extends BladeController {

	private final IPropertyRegionService propertyRegionService;

	private final SyncAddressExecutor syncAddressExecutor;

	/**
	 * 物业信息表 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入propertyRegion")
	public R<PropertyRegionVO> detail(PropertyRegionEntity propertyRegion) {
		PropertyRegionEntity detail = propertyRegionService.getOne(Condition.getQueryWrapper(propertyRegion));
		return R.data(PropertyRegionWrapper.build().entityVO(detail));
	}
	/**
	 * 物业信息表 分页
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入propertyRegion")
	public R<IPage<PropertyRegionVO>> list(@RequestParam Map<String, Object> propertyRegion, Query query) {
		IPage<PropertyRegionEntity> pages = propertyRegionService.page(Condition.getPage(query), Condition.getQueryWrapper(propertyRegion, PropertyRegionEntity.class));
		return R.data(PropertyRegionWrapper.build().pageVO(pages));
	}


	@PostMapping("/dict")
	@ApiOperationSupport(order = 2)
	public R<List<PropertyRegionVO>> list() {
		List<PropertyRegionEntity> list = propertyRegionService.list();
		return R.data(PropertyRegionWrapper.build().listVO(list));
	}


	/**
	 * 物业信息表 自定义分页
	 */
	@PostMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入propertyRegion")
	public R<IPage<PropertyRegionVO>> page(PropertyRegionVO propertyRegion, Query query) {
		IPage<PropertyRegionVO> pages = propertyRegionService.selectPropertyRegionPage(Condition.getPage(query), propertyRegion);
		return R.data(pages);
	}

	/**
	 * 物业信息表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入propertyRegion")
	public R save(@Valid @RequestBody PropertyRegionEntity propertyRegion) {
		return R.status(propertyRegionService.save(propertyRegion));
	}


	@PostMapping("/sync")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "同步数据")
	public R doSync() {
		syncAddressExecutor.doSyncAddressDataByInterface(BizScheduledTriggerModeEnum.TASK_SYNC_HAND.getType());
		return R.status(true);
	}


	/**
	 * 物业信息表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入propertyRegion")
	public R update(@Valid @RequestBody PropertyRegionEntity propertyRegion) {
		return R.status(propertyRegionService.updateById(propertyRegion));
	}

	/**
	 * 物业信息表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入propertyRegion")
	public R submit(@Valid @RequestBody PropertyRegionEntity propertyRegion) {
		return R.status(propertyRegionService.submit(propertyRegion));
	}

	/**
	 * 物业信息表 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(propertyRegionService.deleteLogic(Func.toLongList(ids)));
	}


	/**
	 * 导出数据
	 */
	@PostMapping("/export-propertyRegion")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "导出数据", description = "传入propertyRegion")
	public void exportPropertyRegion(@RequestParam Map<String, Object> propertyRegion, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<PropertyRegionEntity> queryWrapper = Condition.getQueryWrapper(propertyRegion, PropertyRegionEntity.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(PropertyRegion::getTenantId, bladeUser.getTenantId());
		//}
		queryWrapper.lambda().eq(PropertyRegionEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<PropertyRegionExcel> list = propertyRegionService.exportPropertyRegion(queryWrapper);
		ExcelUtil.export(response, "物业信息表数据" + DateUtil.time(), "物业信息表数据表", list, PropertyRegionExcel.class);
	}

}
