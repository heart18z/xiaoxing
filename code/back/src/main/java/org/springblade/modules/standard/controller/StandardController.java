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
package org.springblade.modules.standard.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springblade.modules.standard.entity.StandardEntity;
import org.springblade.modules.standard.excel.StandardExcel;
import org.springblade.modules.standard.service.IStandardService;
import org.springblade.modules.standard.vo.StandardNodeVO;
import org.springblade.modules.standard.vo.StandardVO;
import org.springblade.modules.standard.wrapper.StandardWrapper;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 规范类列表 控制器
 *
 * @author linchaofan
 * @since 2023-06-09
 */
@RestController
@AllArgsConstructor
@RequestMapping("blade-standard/standard")
@Tag(name = "规范类列表接口", description = "规范类列表")
public class StandardController extends BladeController {

	private final IStandardService standardService;

	/**
	 * 规范类列表 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入standard")
	public R<StandardVO> detail(StandardEntity standard) {
		StandardEntity detail = standardService.getOne(Condition.getQueryWrapper(standard));
		return R.data(StandardWrapper.build().entityVO(detail));
	}
	/**
	 * 规范类列表 分页
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入standard")
	public R<IPage<StandardVO>> list(@RequestParam Map<String, Object> standard, Query query) {
		IPage<StandardEntity> pages = standardService.page(Condition.getPage(query), Condition.getQueryWrapper(standard, StandardEntity.class));
		return R.data(StandardWrapper.build().pageVO(pages));
	}

	/**
	 * 规范类列表 自定义分页
	 */
	@PostMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入standard")
	public R<IPage<StandardVO>> page(StandardVO standard, Query query) {
		IPage<StandardVO> pages = standardService.selectStandardPage(Condition.getPage(query), standard);
		return R.data(pages);
	}

	/**
	 * 规范类列表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入standard")
	public R save(@Valid @RequestBody StandardEntity standard) {
		return R.status(standardService.save(standard));
	}

	/**
	 * 规范类列表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入standard")
	public R update(@Valid @RequestBody StandardEntity standard) {
		return R.status(standardService.updateById(standard));
	}

	/**
	 * 规范类列表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入standard")
	public R submit(@Valid @RequestBody StandardEntity standard) {
		return R.status(standardService.submit(standard));
	}

	/**
	 * 规范类列表 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(standardService.deleteLogic(Func.toLongList(ids)));
	}


	/**
	 * 导出数据
	 */
	@PostMapping("/export-standard")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "导出数据", description = "传入standard")
	public void exportStandard(@RequestParam Map<String, Object> standard, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<StandardEntity> queryWrapper = Condition.getQueryWrapper(standard, StandardEntity.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(Standard::getTenantId, bladeUser.getTenantId());
		//}
		queryWrapper.lambda().eq(StandardEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<StandardExcel> list = standardService.exportStandard(queryWrapper);
		ExcelUtil.export(response, "规范类列表数据" + DateUtil.time(), "规范类列表数据表", list, StandardExcel.class);
	}



	/**
	 * 获取菜单树形结构
	 */
	@PostMapping("/tree")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "树形结构", description = "树形结构")
	public R<List<StandardNodeVO>> tree() {
		List<StandardNodeVO> tree = standardService.tree();
		return R.data(tree);
	}

	/**
	 * 懒加载菜单列表
	 */
	@PostMapping("/lazy-list")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "懒加载列表", description = "传入standard")
	public R<List<StandardVO>> lazyList(Long parentId, @RequestParam Map<String, Object> param) {
		List<StandardVO> list = standardService.lazyList(parentId, param);
		return R.data(list);
	}


	/**
	 * 懒加载分页菜单列表
	 */
	@PostMapping("/lazy-page")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "懒加载分页列表", description = "传入standard")
	public R<IPage<StandardVO>> lazyPage(Long parentId, @RequestParam Map<String, Object> param, Query query) {
		param.put("current", query.getCurrent() == null ? 0 : (query.getCurrent()-1)*(query.getSize() == null ? 10 : query.getSize()));
		param.put("size", query.getSize() == null ? 10 : query.getSize());
		List<StandardVO> list = standardService.lazyList(parentId, param);
		IPage<StandardVO> standardVOIPage = Condition.getPage(query);
		standardVOIPage.setTotal(standardService.lazyListCount(parentId, param));
		standardVOIPage.setRecords(list);
		return R.data(standardVOIPage);
	}


}
