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
package org.springblade.modules.aiconfig.controller;

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
import org.springblade.modules.aiconfig.entity.BtnConfigDetailEntity;
import org.springblade.modules.aiconfig.excel.BtnConfigDetailExcel;
import org.springblade.modules.aiconfig.service.IBtnConfigDetailService;
import org.springblade.modules.aiconfig.vo.BtnConfigDetailVO;
import org.springblade.modules.aiconfig.wrapper.BtnConfigDetailWrapper;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * AI按钮接口配置详情表 控制器
 *
 * @author wxd
 * @since 2026-03-30
 */
@RestController
@AllArgsConstructor
@RequestMapping("blade-ai/btnConfigDetail")
@Tag(name = "AI按钮接口配置详情表接口", description = "AI按钮接口配置详情表")
public class BtnConfigDetailController extends BladeController {

	private final IBtnConfigDetailService btnConfigDetailService;

	/**
	 * AI按钮接口配置详情表 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入btnConfigDetail")
	public R<BtnConfigDetailVO> detail(BtnConfigDetailEntity btnConfigDetail) {
		BtnConfigDetailEntity detail = btnConfigDetailService.getOne(Condition.getQueryWrapper(btnConfigDetail));
		return R.data(BtnConfigDetailWrapper.build().entityVO(detail));
	}
	/**
	 * AI按钮接口配置详情表 分页
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入btnConfigDetail")
	public R<IPage<BtnConfigDetailVO>> list(@RequestParam Map<String, Object> btnConfigDetail, Query query) {
		IPage<BtnConfigDetailEntity> pages = btnConfigDetailService.page(Condition.getPage(query), Condition.getQueryWrapper(btnConfigDetail, BtnConfigDetailEntity.class));
		return R.data(BtnConfigDetailWrapper.build().pageVO(pages));
	}

	/**
	 * AI按钮接口配置详情表 自定义分页
	 */
	@PostMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入btnConfigDetail")
	public R<IPage<BtnConfigDetailVO>> page(BtnConfigDetailVO btnConfigDetail, Query query) {
		IPage<BtnConfigDetailVO> pages = btnConfigDetailService.selectBtnConfigDetailPage(Condition.getPage(query), btnConfigDetail);
		return R.data(pages);
	}

	/**
	 * AI按钮接口配置详情表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入btnConfigDetail")
	public R save(@Valid @RequestBody BtnConfigDetailEntity btnConfigDetail) {
		return R.status(btnConfigDetailService.save(btnConfigDetail));
	}

	/**
	 * AI按钮接口配置详情表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入btnConfigDetail")
	public R update(@Valid @RequestBody BtnConfigDetailEntity btnConfigDetail) {
		return R.status(btnConfigDetailService.updateById(btnConfigDetail));
	}

	/**
	 * AI按钮接口配置详情表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入btnConfigDetail")
	public R submit(@Valid @RequestBody BtnConfigDetailEntity btnConfigDetail) {
		return R.status(btnConfigDetailService.saveOrUpdate(btnConfigDetail));
	}

	/**
	 * AI按钮接口配置详情表 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(btnConfigDetailService.deleteLogic(Func.toLongList(ids)));
	}


	/**
	 * 导出数据
	 */
	@PostMapping("/export-btnConfigDetail")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "导出数据", description = "传入btnConfigDetail")
	public void exportBtnConfigDetail(@RequestParam Map<String, Object> btnConfigDetail, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<BtnConfigDetailEntity> queryWrapper = Condition.getQueryWrapper(btnConfigDetail, BtnConfigDetailEntity.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(BtnConfigDetail::getTenantId, bladeUser.getTenantId());
		//}
		queryWrapper.lambda().eq(BtnConfigDetailEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<BtnConfigDetailExcel> list = btnConfigDetailService.exportBtnConfigDetail(queryWrapper);
		ExcelUtil.export(response, "AI按钮接口配置详情表数据" + DateUtil.time(), "AI按钮接口配置详情表数据表", list, BtnConfigDetailExcel.class);
	}

}
