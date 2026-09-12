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
import org.springblade.modules.system.entity.NoteEntity;
import org.springblade.modules.system.vo.NoteVO;
import org.springblade.modules.system.excel.NoteExcel;
import org.springblade.modules.system.wrapper.NoteWrapper;
import org.springblade.modules.system.service.INoteService;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.tool.constant.BladeConstant;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 注释 控制器
 *
 * @author BladeX
 * @since 2024-01-17
 */
@RestController
@AllArgsConstructor
@RequestMapping("blade-note/note")
@Tag(name = "注释接口", description = "注释")
public class NoteController extends BladeController {

	private final INoteService noteService;

	/**
	 * 注释 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入note")
	public R<NoteVO> detail(NoteEntity note) {
		NoteEntity detail = noteService.getOne(Condition.getQueryWrapper(note));
		return R.data(NoteWrapper.build().entityVO(detail));
	}
	/**
	 * 注释 分页
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入note")
	public R<IPage<NoteVO>> list(@RequestParam Map<String, Object> note, Query query) {
		IPage<NoteEntity> pages = noteService.page(Condition.getPage(query), Condition.getQueryWrapper(note, NoteEntity.class)
			.lambda().orderByDesc(NoteEntity::getCreateTime));
		return R.data(NoteWrapper.build().pageVO(pages));
	}

	/**
	 * 注释 自定义分页
	 */
	@PostMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入note")
	public R<IPage<NoteVO>> page(NoteVO note, Query query) {
		IPage<NoteVO> pages = noteService.selectNotePage(Condition.getPage(query), note);
		return R.data(pages);
	}

	/**
	 * 注释 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入note")
	public R save(@Valid @RequestBody NoteEntity note) {
		return R.status(noteService.save(note));
	}

	/**
	 * 注释 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入note")
	public R update(@Valid @RequestBody NoteEntity note) {
		return R.status(noteService.updateById(note));
	}

	/**
	 * 注释 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入note")
	public R submit(@Valid @RequestBody NoteEntity note) {
		return R.status(noteService.saveOrUpdate(note));
	}

	/**
	 * 注释 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(noteService.deleteLogic(Func.toLongList(ids)));
	}


	/**
	 * 导出数据
	 */
	@PostMapping("/export-note")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "导出数据", description = "传入note")
	public void exportNote(@RequestParam Map<String, Object> note, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<NoteEntity> queryWrapper = Condition.getQueryWrapper(note, NoteEntity.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(Note::getTenantId, bladeUser.getTenantId());
		//}
		queryWrapper.lambda().eq(NoteEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<NoteExcel> list = noteService.exportNote(queryWrapper);
		ExcelUtil.export(response, "注释数据" + DateUtil.time(), "注释数据表", list, NoteExcel.class);
	}

	/**
	 * 注释 详情
	 */
	@PostMapping("/select")

	public R<List<NoteVO>> select(NoteEntity note) {
		List<NoteEntity> list = noteService.list();
		return R.data(NoteWrapper.build().listVO(list));
	}



}
