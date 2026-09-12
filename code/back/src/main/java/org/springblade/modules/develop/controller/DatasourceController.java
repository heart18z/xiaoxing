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
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.develop.entity.Datasource;
import org.springblade.modules.develop.service.IDatasourceService;
import org.springblade.modules.develop.vo.TableInfoVO;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 数据源配置表 控制器
 *
 * @author Chill
 */
@NonDS
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_DEVELOP_NAME + "/datasource")
@Tag(name = "数据源配置表接口", description = "数据源配置表")
public class DatasourceController extends BladeController {

	private final IDatasourceService datasourceService;

	/**
	 * 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入datasource")
	public R<Datasource> detail(Datasource datasource) {
		Datasource detail = datasourceService.getOne(Condition.getQueryWrapper(datasource));
		detail.setPassword("");
		return R.data(detail);
	}

	/**
	 * 分页 数据源配置表
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入datasource")
	public R<IPage<Datasource>> list(Datasource datasource, Query query) {
		IPage<Datasource> pages = datasourceService.page(Condition.getPage(query), Condition.getQueryWrapper(datasource));
		pages.getRecords().forEach(item->item.setPassword(""));
		return R.data(pages);
	}



	/**
	 * 新增或修改 数据源配置表
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入datasource")
	public R submit(@Valid @RequestBody Datasource datasource) {
		if (Func.isNotBlank(datasource.getUrl())) {
			datasource.setUrl(datasource.getUrl().replace("&amp;", "&"));
		}
		return R.status(datasourceService.submit(datasource));
	}


	/**
	 * 删除 数据源配置表
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(datasourceService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * 数据源列表
	 */
	@PostMapping("/select")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "下拉数据源", description = "查询列表")
	public R<List<Datasource>> select() {
		List<Datasource> list = datasourceService.list();
		list.forEach(item->item.setPassword(""));
		return R.data(list);
	}


	/**
	 * 分页 获取业务库表配置好的 数据源配置表
	 */
	@PostMapping("/select-by-menu-table")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "下拉数据源", description = "查询列表")
	public R<List<Datasource>> listByMenuTable() {
		List<Datasource> list = datasourceService.listByMenuTable();
		list.forEach(item->item.setPassword(""));
		return R.data(list);
	}

	/**
	 * 获取物理表列表
	 */
	@PostMapping("/table-list-by-menu-table")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "物理表列表", description = "传入datasourceId")
	public R<List<TableInfoVO>> tableListByMenuTable(Long datasourceId) {
		List<TableInfoVO> list = datasourceService.getTableListByMenuTable(datasourceId);
		return R.data(list);
	}

}
