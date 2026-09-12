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
package org.springblade.modules.desk.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import lombok.AllArgsConstructor;
import jakarta.validation.Valid;

import org.springblade.common.constant.MessageConstant;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.modules.desk.entity.UnifiedMessagingSendEntity;
import org.springblade.modules.desk.vo.UnifiedMessagingSendVO;
import org.springblade.modules.desk.excel.UnifiedMessagingSendExcel;
import org.springblade.modules.desk.wrapper.UnifiedMessagingSendWrapper;
import org.springblade.modules.desk.service.IUnifiedMessagingSendService;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.tool.constant.BladeConstant;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Date;
import java.util.Map;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 统一消息发送表 控制器
 *
 * @author BladeX
 * @since 2023-12-11
 */
@RestController
@AllArgsConstructor
@RequestMapping("blade-unifiedMessagingSend/unifiedMessagingSend")
@Tag(name = "统一消息发送表接口", description = "统一消息发送表")
public class UnifiedMessagingSendController extends BladeController {

	private final IUnifiedMessagingSendService unifiedMessagingSendService;

	/**
	 * 统一消息发送表 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入unifiedMessagingSend")
	public R<UnifiedMessagingSendVO> detail(UnifiedMessagingSendEntity unifiedMessagingSend) {
		UnifiedMessagingSendEntity detail = unifiedMessagingSendService.getOne(Condition.getQueryWrapper(unifiedMessagingSend));
		return R.data(UnifiedMessagingSendWrapper.build().entityVO(detail));
	}
	/**
	 * 统一消息发送表 分页
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入unifiedMessagingSend")
	public R<IPage<UnifiedMessagingSendVO>> list(@RequestParam Map<String, Object> unifiedMessagingSend, Query query) {

		return R.data(unifiedMessagingSendService.pageQuery(query, unifiedMessagingSend));
	}

	/**
	 * 统一消息发送表 自定义分页
	 */
	@PostMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入unifiedMessagingSend")
	public R<IPage<UnifiedMessagingSendVO>> page(UnifiedMessagingSendVO unifiedMessagingSend, Query query) {
		IPage<UnifiedMessagingSendVO> pages = unifiedMessagingSendService.selectUnifiedMessagingSendPage(Condition.getPage(query), unifiedMessagingSend);
		return R.data(pages);
	}

	/**
	 * 统一消息发送表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入unifiedMessagingSend")
	public R save(@Valid @RequestBody UnifiedMessagingSendEntity unifiedMessagingSend) {
		return R.status(unifiedMessagingSendService.save(unifiedMessagingSend));
	}

	/**
	 * 统一消息发送表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入unifiedMessagingSend")
	public R update(@Valid @RequestBody UnifiedMessagingSendEntity unifiedMessagingSend) {
		if(!MessageConstant.MESSAGE_RATE_DATE_NOW.equals(unifiedMessagingSend.getSendRate())) {
			if (unifiedMessagingSend.getEndSentTime()!=null && unifiedMessagingSend.getEndSentTime().getTime()<new Date().getTime()) {
				throw new ServiceException("截止时间小于当前时间，操作失败！");
			}
		}
		return R.status(unifiedMessagingSendService.updateById(unifiedMessagingSend));
	}

	/**
	 * 统一消息发送表 修改
	 */
	@PostMapping("/change-status")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入unifiedMessagingSend")
	public R changeStatus(@Valid @RequestBody UnifiedMessagingSendVO unifiedMessagingSend) {

		return R.status(unifiedMessagingSendService.changeMessageStatus(unifiedMessagingSend));
	}



	/**
	 * 统一消息发送表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入unifiedMessagingSend")
	public R submit(@Valid @RequestBody UnifiedMessagingSendVO unifiedMessagingSend) {
		return R.status(unifiedMessagingSendService.submit(unifiedMessagingSend));
	}

	/**
	 * 统一消息发送表 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(unifiedMessagingSendService.deleteLogic(Func.toLongList(ids)));
	}


	/**
	 * 导出数据
	 */
	@PostMapping("/export-unifiedMessagingSend")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "导出数据", description = "传入unifiedMessagingSend")
	public void exportUnifiedMessagingSend(@RequestParam Map<String, Object> unifiedMessagingSend, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<UnifiedMessagingSendEntity> queryWrapper = Condition.getQueryWrapper(unifiedMessagingSend, UnifiedMessagingSendEntity.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(UnifiedMessagingSend::getTenantId, bladeUser.getTenantId());
		//}
		queryWrapper.lambda().eq(UnifiedMessagingSendEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<UnifiedMessagingSendExcel> list = unifiedMessagingSendService.exportUnifiedMessagingSend(queryWrapper);
		ExcelUtil.export(response, "统一消息发送表数据" + DateUtil.time(), "统一消息发送表数据表", list, UnifiedMessagingSendExcel.class);
	}

}
