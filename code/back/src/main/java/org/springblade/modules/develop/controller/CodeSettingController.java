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
package org.springblade.modules.develop.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.develop.entity.CodeSetting;
import org.springblade.modules.develop.service.ICodeSettingService;
import org.springblade.modules.develop.service.IModelPrototypeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 代码生成器配置表 控制器
 *
 * @author Chill
 */
@NonDS
@Hidden
@RestController
@AllArgsConstructor
@PreAuth(RoleConstant.HAS_ROLE_ADMINISTRATOR)
@RequestMapping(AppConstant.APPLICATION_DEVELOP_NAME + "/code-setting")
@Tag(name = "代码生成器配置表", description = "代码生成器配置表接口")
public class CodeSettingController extends BladeController {

	private final ICodeSettingService codeSettingService;
	private final IModelPrototypeService modelPrototypeService;

	/**
	 * 代码生成器配置表 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入codeSetting")
	public R<CodeSetting> detail(CodeSetting codeSetting) {
		CodeSetting detail = codeSettingService.getOne(Condition.getQueryWrapper(codeSetting));
		return R.data(detail);
	}

	/**
	 * 代码生成器配置表 分页
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入codeSetting")
	public R<IPage<CodeSetting>> list(@RequestParam Map<String, Object> codeSetting, Query query) {
		IPage<CodeSetting> pages = codeSettingService.page(Condition.getPage(query), Condition.getQueryWrapper(codeSetting, CodeSetting.class).orderByDesc("id"));
		return R.data(pages);
	}

	/**
	 * 代码生成器配置表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "新增或修改", description = "传入codeSetting")
	public R submit(@Valid @RequestBody CodeSetting codeSetting) {
		boolean temp = codeSettingService.saveOrUpdate(codeSetting);
		if (temp) {
			return R.data(codeSetting);
		}
		return R.status(Boolean.FALSE);
	}

	/**
	 * 代码生成器配置表 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(codeSettingService.removeByIds(Func.toLongList(ids)));
	}

	/**
	 * 代码生成器配置表 启用
	 */
	@PostMapping("/enable")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "配置启用", description = "传入id")
	public R enable(@Parameter(description = "主键", required = true) @RequestParam Long id) {
		return R.status(codeSettingService.enable(id));
	}

	/**
	 * 代码生成器配置表 启用详情
	 */
	@PostMapping("/enable-detail")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "启用配置详情", description = "获取当前启用的配置")
	public R<CodeSetting> enableDetail() {
		CodeSetting detail = codeSettingService.getOne(Wrappers.<CodeSetting>lambdaQuery()
			.eq(CodeSetting::getStatus, BladeConstant.DB_STATUS_2)
			.eq(CodeSetting::getIsDeleted, BladeConstant.DB_NOT_DELETED));
		return R.data(detail);
	}

	/**
	 * 表单设计器选择
	 */
	@PostMapping("/table-form")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "表单设计器选择", description = "tableName")
	public R<List<CodeSetting>> formSelect(String tableName) {
		return R.data(codeSettingService.list(Wrappers.<CodeSetting>lambdaQuery()
			.eq(CodeSetting::getCode, tableName)
			.eq(CodeSetting::getCategory, 2)));
	}

	/**
	 * 获取字段信息
	 */
	@PostMapping("/table-prototype")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "物理表字段信息", description = "传入tableName与datasourceId")
	public R tablePrototype(String tableName, Long datasourceId) {
		TableInfo tableInfo = modelPrototypeService.getTableInfo(tableName, datasourceId);
		if (tableInfo != null) {
			return R.data(tableInfo.getFields());
		}
		return R.fail("未获得相关表信息");
	}

}
