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
import org.springblade.modules.system.entity.NoteEntity;
import org.springblade.modules.system.vo.NoteVO;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.modules.system.entity.NoteSettingEntity;
import org.springblade.modules.system.vo.NoteSettingVO;
import org.springblade.modules.system.excel.NoteSettingExcel;
import org.springblade.modules.system.wrapper.NoteSettingWrapper;
import org.springblade.modules.system.service.INoteSettingService;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.tool.constant.BladeConstant;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 注释设置 控制器
 *
 * @author BladeX
 * @since 2024-01-17
 */
@RestController
@AllArgsConstructor
@RequestMapping("blade-noteSetting/noteSetting")
@Tag(name = "注释设置接口", description = "注释设置")
public class NoteSettingController extends BladeController {

	private final INoteSettingService noteSettingService;

	/**
	 * 注释设置 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入noteSetting")
	public R<NoteSettingVO> detail(NoteSettingEntity noteSetting) {
		NoteSettingEntity detail = noteSettingService.getOne(Condition.getQueryWrapper(noteSetting));
		return R.data(NoteSettingWrapper.build().entityVO(detail));
	}
	/**
	 * 注释设置 分页
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入noteSetting")
	public R<IPage<NoteSettingVO>> list(@RequestParam Map<String, Object> noteSetting, Query query) {
		IPage<NoteSettingEntity> pages = noteSettingService.page(Condition.getPage(query), Condition.getQueryWrapper(noteSetting, NoteSettingEntity.class));
		return R.data(NoteSettingWrapper.build().pageVO(pages));
	}

	/**
	 * 注释设置 自定义分页
	 */
	@PostMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入noteSetting")
	public R<IPage<NoteSettingVO>> page(NoteSettingVO noteSetting, Query query) {
		IPage<NoteSettingVO> pages = noteSettingService.selectNoteSettingPage(Condition.getPage(query), noteSetting);
		return R.data(pages);
	}

	/**
	 * 注释设置 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入noteSetting")
	public R save(@Valid @RequestBody NoteSettingEntity noteSetting) {
		return R.status(noteSettingService.save(noteSetting));
	}

	/**
	 * 注释设置 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入noteSetting")
	public R update(@Valid @RequestBody NoteSettingEntity noteSetting) {
		return R.status(noteSettingService.updateById(noteSetting));
	}

	/**
	 * 注释设置 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入noteSetting")
	public R submit(@Valid @RequestBody NoteSettingEntity noteSetting) {
		return R.status(noteSettingService.submit(noteSetting));
	}

	/**
	 * 注释设置 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(noteSettingService.deleteLogic(Func.toLongList(ids)));
	}


	/**
	 * 导出数据
	 */
	@PostMapping("/export-noteSetting")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "导出数据", description = "传入noteSetting")
	public void exportNoteSetting(@RequestParam Map<String, Object> noteSetting, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<NoteSettingEntity> queryWrapper = Condition.getQueryWrapper(noteSetting, NoteSettingEntity.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(NoteSetting::getTenantId, bladeUser.getTenantId());
		//}
		queryWrapper.lambda().eq(NoteSettingEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<NoteSettingExcel> list = noteSettingService.exportNoteSetting(queryWrapper);
		ExcelUtil.export(response, "注释设置数据" + DateUtil.time(), "注释设置数据表", list, NoteSettingExcel.class);
	}

	@PostMapping("/select-dom-note")
	public R<List<NoteSettingVO>> selectDomNote(NoteSettingVO note) {
		return R.data(noteSettingService.selectDomNote(note));

	}

}
