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

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springblade.common.constant.CommonConstant;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.cache.utils.CacheUtil;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.utils.StringPool;
import org.springblade.modules.system.entity.Dict;
import org.springblade.modules.system.service.IDictService;
import org.springblade.modules.system.vo.DictVO;
import org.springblade.modules.system.wrapper.DictWrapper;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

import static org.springblade.core.cache.constant.CacheConstant.DICT_CACHE;

/**
 * 控制器
 *
 * @author Chill
 */
@NonDS
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_SYSTEM_NAME + "/dict")
@Tag(name = "系统字典", description = "系统字典")
public class DictController extends BladeController {

	private final IDictService dictService;

	/**
	 * 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入dict")
	@PreAuth("hasPermission('blade:dict:detail')")
	public R<DictVO> detail(Dict dict) {
		Dict detail = dictService.getOne(Condition.getQueryWrapper(dict));
		return R.data(DictWrapper.build().entityVO(detail));
	}

	/**
	 * 列表
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "列表", description = "传入dict")
	public R<List<DictVO>> list(@RequestParam Map<String, Object> dict) {
		List<Dict> list = dictService.list(Condition.getQueryWrapper(dict, Dict.class).lambda().orderByAsc(Dict::getSort));
		return R.data(DictWrapper.build().listNodeVO(list));
	}

	/**
	 * 顶级列表
	 */
	@PostMapping("/parent-list")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "列表", description = "传入dict")
	@PreAuth("hasPermission('blade:dict:parent-list')")
	public R<IPage<DictVO>> parentList(@RequestParam Map<String, Object> dict, Query query) {
		return R.data(dictService.parentList(dict, query));
	}

	/**
	 * 子列表
	 */
	@PostMapping("/child-list")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "列表", description = "传入dict")
	@PreAuth("hasPermission('blade:dict:child-list')")
	public R<List<DictVO>> childList(@RequestParam Map<String, Object> dict, @RequestParam(required = false, defaultValue = "-1") Long parentId) {
		return R.data(dictService.childList(dict, parentId));
	}

	/**
	 * 获取字典树形结构
	 */
	@PostMapping("/tree")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "树形结构", description = "树形结构")
	public R<List<DictVO>> tree() {
		List<DictVO> tree = dictService.tree();
		return R.data(tree);
	}

	/**
	 * 获取字典树形结构
	 */
	@PostMapping("/parent-tree")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "树形结构", description = "树形结构")
	public R<List<DictVO>> parentTree() {
		List<DictVO> tree = dictService.parentTree();
		return R.data(tree);
	}

	/**
	 * 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@PreAuth("hasPermission('blade:dict:submit')")
	public R submit(@Valid @RequestBody Dict dict) {
		CacheUtil.clear(DICT_CACHE, Boolean.FALSE);
		return R.status(dictService.submit(dict));
	}


	/**
	 * 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "删除", description = "传入ids")
	@PreAuth("hasPermission('blade:dict:remove')")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		CacheUtil.clear(DICT_CACHE, Boolean.FALSE);
		return R.status(dictService.removeDict(ids));
	}

	/**
	 * 获取字典
	 */
	@PostMapping("/dictionary")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "获取字典", description = "获取字典")
	public R<List<Dict>> dictionary(String code) {
		List<Dict> tree = dictService.getList(code);
		return R.data(tree);
	}

	/**
	 * 获取字典树
	 */
	@PostMapping("/dictionary-tree")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "获取字典树", description = "获取字典树")
	public R<List<DictVO>> dictionaryTree(String code) {
		List<Dict> tree = dictService.getList(code);
		return R.data(DictWrapper.build().listNodeVO(tree));
	}

	/**
	 * 字典键值列表
	 */
	@PostMapping("/select")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "字典键值列表", description = "字典键值列表")
	public R<List<Dict>> select() {
		List<Dict> list = dictService.list(Wrappers.<Dict>query().lambda().eq(Dict::getParentId, CommonConstant.TOP_PARENT_ID));
		list.forEach(dict -> dict.setDictValue(dict.getCode() + StringPool.COLON + StringPool.SPACE + dict.getDictValue()));
		return R.data(list);
	}

	/**
	 * 字典全列表
	 */
	@PostMapping("/select-all")
	@ApiOperationSupport(order = 11)
	@Operation(summary = "字典全列表", description = "字典全列表")
	public R<List<Dict>> selectAll() {
		List<Dict> list = dictService.list(Wrappers.<Dict>query().lambda().eq(Dict::getIsDeleted, BladeConstant.DB_NOT_DELETED));
		return R.data(list);
	}

}
