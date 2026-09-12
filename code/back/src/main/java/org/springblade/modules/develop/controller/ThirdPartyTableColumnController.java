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
import org.springblade.modules.develop.entity.ThirdPartyTableColumnEntity;
import org.springblade.modules.develop.excel.ThirdPartyTableColumnExcel;
import org.springblade.modules.develop.vo.ThirdPartyTableColumnVO;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.modules.develop.wrapper.ThirdPartyTableColumnWrapper;
import org.springblade.modules.develop.service.IThirdPartyTableColumnService;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.tool.constant.BladeConstant;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 数据中台列信息 控制器
 *
 * @author BladeX
 * @since 2025-02-09
 */
@RestController
@AllArgsConstructor
@RequestMapping("blade-thirdPartyTableColumn/thirdPartyTableColumn")
@Tag(name = "数据中台列信息接口", description = "数据中台列信息")
public class ThirdPartyTableColumnController extends BladeController {

	private final IThirdPartyTableColumnService thirdPartyTableColumnService;

	/**
	 * 数据中台列信息 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入thirdPartyTableColumn")
	public R<ThirdPartyTableColumnVO> detail(ThirdPartyTableColumnEntity thirdPartyTableColumn) {
		ThirdPartyTableColumnEntity detail = thirdPartyTableColumnService.getOne(Condition.getQueryWrapper(thirdPartyTableColumn));
		return R.data(ThirdPartyTableColumnWrapper.build().entityVO(detail));
	}
	/**
	 * 数据中台列信息 分页
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入thirdPartyTableColumn")
	public R<IPage<ThirdPartyTableColumnVO>> list(@RequestParam Map<String, Object> thirdPartyTableColumn, Query query) {
		IPage<ThirdPartyTableColumnEntity> pages = thirdPartyTableColumnService.page(Condition.getPage(query), Condition.getQueryWrapper(thirdPartyTableColumn, ThirdPartyTableColumnEntity.class));
		return R.data(ThirdPartyTableColumnWrapper.build().pageVO(pages));
	}

	/**
	 * 数据中台列信息 自定义分页
	 */
	@PostMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入thirdPartyTableColumn")
	public R<IPage<ThirdPartyTableColumnVO>> page(ThirdPartyTableColumnVO thirdPartyTableColumn, Query query) {
		IPage<ThirdPartyTableColumnVO> pages = thirdPartyTableColumnService.selectThirdPartyTableColumnPage(Condition.getPage(query), thirdPartyTableColumn);
		return R.data(pages);
	}

	/**
	 * 数据中台列信息 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入thirdPartyTableColumn")
	public R save(@Valid @RequestBody ThirdPartyTableColumnEntity thirdPartyTableColumn) {
		return R.status(thirdPartyTableColumnService.save(thirdPartyTableColumn));
	}

	/**
	 * 数据中台列信息 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入thirdPartyTableColumn")
	public R update(@Valid @RequestBody ThirdPartyTableColumnEntity thirdPartyTableColumn) {
		return R.status(thirdPartyTableColumnService.updateById(thirdPartyTableColumn));
	}

	/**
	 * 数据中台列信息 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入thirdPartyTableColumn")
	public R submit(@Valid @RequestBody ThirdPartyTableColumnEntity thirdPartyTableColumn) {
		return R.status(thirdPartyTableColumnService.saveOrUpdate(thirdPartyTableColumn));
	}

	/**
	 * 数据中台列信息 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(thirdPartyTableColumnService.deleteLogic(Func.toLongList(ids)));
	}


	/**
	 * 导出数据
	 */
	@PostMapping("/export-thirdPartyTableColumn")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "导出数据", description = "传入thirdPartyTableColumn")
	public void exportThirdPartyTableColumn(@RequestParam Map<String, Object> thirdPartyTableColumn, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<ThirdPartyTableColumnEntity> queryWrapper = Condition.getQueryWrapper(thirdPartyTableColumn, ThirdPartyTableColumnEntity.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(ThirdPartyTableColumn::getTenantId, bladeUser.getTenantId());
		//}
		queryWrapper.lambda().eq(ThirdPartyTableColumnEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<ThirdPartyTableColumnExcel> list = thirdPartyTableColumnService.exportThirdPartyTableColumn(queryWrapper);
		ExcelUtil.export(response, "数据中台列信息数据" + DateUtil.time(), "数据中台列信息数据表", list, ThirdPartyTableColumnExcel.class);
	}

}
