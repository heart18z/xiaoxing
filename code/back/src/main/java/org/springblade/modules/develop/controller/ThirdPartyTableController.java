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
import org.springblade.modules.develop.entity.ThirdPartyTableEntity;
import org.springblade.modules.develop.vo.ThirdPartyTableVO;
import org.springblade.modules.develop.excel.ThirdPartyTableExcel;
import org.springblade.modules.develop.wrapper.ThirdPartyTableWrapper;
import org.springblade.modules.develop.service.IThirdPartyTableService;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.tool.constant.BladeConstant;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 中台数据 控制器
 *
 * @author BladeX
 * @since 2025-02-09
 */
@RestController
@AllArgsConstructor
@RequestMapping("blade-thirdPartyTable/thirdPartyTable")
@Tag(name = "中台数据接口", description = "中台数据")
public class ThirdPartyTableController extends BladeController {

	private final IThirdPartyTableService thirdPartyTableService;

	/**
	 * 中台数据 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入thirdPartyTable")
	public R<ThirdPartyTableVO> detail(ThirdPartyTableEntity thirdPartyTable) {
		ThirdPartyTableVO detail = thirdPartyTableService.detail(thirdPartyTable.getId());
		return R.data(detail);
	}
	/**
	 * 中台数据 分页
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入thirdPartyTable")
	public R<IPage<ThirdPartyTableVO>> list(@RequestParam Map<String, Object> thirdPartyTable, Query query) {
		IPage<ThirdPartyTableEntity> pages = thirdPartyTableService.page(Condition.getPage(query), Condition.getQueryWrapper(thirdPartyTable, ThirdPartyTableEntity.class));
		return R.data(ThirdPartyTableWrapper.build().pageVO(pages));
	}

	/**
	 * 中台数据 自定义分页
	 */
	@PostMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入thirdPartyTable")
	public R<IPage<ThirdPartyTableVO>> page(ThirdPartyTableVO thirdPartyTable, Query query) {
		IPage<ThirdPartyTableVO> pages = thirdPartyTableService.selectThirdPartyTablePage(Condition.getPage(query), thirdPartyTable);
		return R.data(pages);
	}

	/**
	 * 中台数据 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入thirdPartyTable")
	public R save(@Valid @RequestBody ThirdPartyTableEntity thirdPartyTable) {
		return R.status(thirdPartyTableService.save(thirdPartyTable));
	}

	/**
	 * 中台数据 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入thirdPartyTable")
	public R update(@Valid @RequestBody ThirdPartyTableEntity thirdPartyTable) {
		return R.status(thirdPartyTableService.updateById(thirdPartyTable));
	}

	/**
	 * 中台数据 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入thirdPartyTable")
	public R submit(@Valid @RequestBody ThirdPartyTableVO thirdPartyTable) {
		return R.status(thirdPartyTableService.submit(thirdPartyTable));
	}

	/**
	 * 中台数据 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(thirdPartyTableService.del(Func.toLongList(ids)));
	}


	/**
	 * 导出数据
	 */
	@PostMapping("/export-thirdPartyTable")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "导出数据", description = "传入thirdPartyTable")
	public void exportThirdPartyTable(@RequestParam Map<String, Object> thirdPartyTable, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<ThirdPartyTableEntity> queryWrapper = Condition.getQueryWrapper(thirdPartyTable, ThirdPartyTableEntity.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(ThirdPartyTable::getTenantId, bladeUser.getTenantId());
		//}
		queryWrapper.lambda().eq(ThirdPartyTableEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<ThirdPartyTableExcel> list = thirdPartyTableService.exportThirdPartyTable(queryWrapper);
		ExcelUtil.export(response, "中台数据数据" + DateUtil.time(), "中台数据数据表", list, ThirdPartyTableExcel.class);
	}

}
