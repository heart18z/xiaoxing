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
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.modules.system.entity.RemoteParamEntity;
import org.springblade.modules.system.vo.RemoteParamVO;
import org.springblade.modules.system.excel.RemoteParamExcel;
import org.springblade.modules.system.wrapper.RemoteParamWrapper;
import org.springblade.modules.system.service.RemoteParamService;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.tool.constant.BladeConstant;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;

/**
 * ai专用应用字典
 控制器
 *
 * @author BladeX
 * @since 2025-03-18
 */
@RestController
@AllArgsConstructor
@RequestMapping("blade-remoteParam/remoteParam")
@Tag(name = "ai专用应用字典接口", description = "ai专用应用字典 ")
public class RemoteParamController extends BladeController {

	private final RemoteParamService remoteParamService;

	/**
	 * ai专用应用字典
 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入remoteParam")
	public R<RemoteParamVO> detail(RemoteParamEntity remoteParam) {
		RemoteParamEntity detail = remoteParamService.getOne(Condition.getQueryWrapper(remoteParam));
		return R.data(RemoteParamWrapper.build().entityVO(detail));
	}
	/**
	 * ai专用应用字典
 分页
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入remoteParam")
	public R<IPage<RemoteParamVO>> list(@RequestParam Map<String, Object> remoteParam, Query query) {
		IPage<RemoteParamEntity> pages = remoteParamService.page(Condition.getPage(query), Condition.getQueryWrapper(remoteParam, RemoteParamEntity.class));
		return R.data(RemoteParamWrapper.build().pageVO(pages));
	}

	/**
	 * ai专用应用字典
 自定义分页
	 */
	@PostMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入remoteParam")
	public R<IPage<RemoteParamVO>> page(RemoteParamVO remoteParam, Query query) {
		IPage<RemoteParamVO> pages = remoteParamService.selectRemoteParamPage(Condition.getPage(query), remoteParam);
		return R.data(pages);
	}

	/**
	 * ai专用应用字典
 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入remoteParam")
	public R save(@Valid @RequestBody RemoteParamEntity remoteParam) {
		return R.status(remoteParamService.save(remoteParam));
	}

	/**
	 * ai专用应用字典
 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入remoteParam")
	public R update(@Valid @RequestBody RemoteParamEntity remoteParam) {
		return R.status(remoteParamService.updateById(remoteParam));
	}

	/**
	 * ai专用应用字典
 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入remoteParam")
	public R submit(@Valid @RequestBody RemoteParamEntity remoteParam) {
		return R.status(remoteParamService.saveOrUpdate(remoteParam));
	}

	/**
	 * ai专用应用字典
 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(remoteParamService.deleteLogic(Func.toLongList(ids)));
	}


	/**
	 * 导出数据
	 */
	@PostMapping("/export-remoteParam")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "导出数据", description = "传入remoteParam")
	public void exportRemoteParam(@RequestParam Map<String, Object> remoteParam, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<RemoteParamEntity> queryWrapper = Condition.getQueryWrapper(remoteParam, RemoteParamEntity.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(RemoteParam::getTenantId, bladeUser.getTenantId());
		//}
		queryWrapper.lambda().eq(RemoteParamEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<RemoteParamExcel> list = remoteParamService.exportRemoteParam(queryWrapper);
		ExcelUtil.export(response, "ai专用应用字典数据" + DateUtil.time(), "ai专用应用字典数据表", list, RemoteParamExcel.class);
	}

}
