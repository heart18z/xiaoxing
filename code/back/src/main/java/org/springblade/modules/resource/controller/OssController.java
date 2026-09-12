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
package org.springblade.modules.resource.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.resource.entity.Oss;
import org.springblade.modules.resource.service.IOssService;
import org.springblade.modules.resource.vo.OssVO;
import org.springblade.modules.resource.wrapper.OssWrapper;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;

import java.util.Map;

import static org.springblade.core.cache.constant.CacheConstant.RESOURCE_CACHE;

/**
 * 控制器
 *
 * @author BladeX
 */
@NonDS
@Hidden
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_RESOURCE_NAME + "/oss")
@Tag(name = "对象存储接口", description = "对象存储接口")
public class OssController extends BladeController {

	private final IOssService ossService;

	/**
	 * 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入oss")
	@PreAuth("hasPermission('blade:oss:detail')")
	public R<OssVO> detail(Oss oss) {
		Oss detail = ossService.getOne(Condition.getQueryWrapper(oss));
		return R.data(OssWrapper.build().entityVO(detail));
	}

	/**
	 * 分页
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入oss")
	@PreAuth("hasPermission('blade:oss:list')")
	public R<IPage<OssVO>> list(@RequestParam Map<String, Object> oss, Query query) {
		IPage<Oss> pages = ossService.page(Condition.getPage(query), Condition.getQueryWrapper(oss,Oss.class));
//				.lambda().eq(Oss::getIsDeleted, BladeConstant.DB_NOT_DELETED)
//			.or(wapper->wapper.eq(Oss::getOssCode, CommonConstant.DEFAULT_OSS_INTRODUCE)));
		return R.data(OssWrapper.build().pageVO(pages));
	}

	/**
	 * 自定义分页
	 */
	@PostMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入oss")
	public R<IPage<OssVO>> page(OssVO oss, Query query) {
		IPage<OssVO> pages = ossService.selectOssPage(Condition.getPage(query), oss);
		return R.data(pages);
	}

	/**
	 * 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入oss")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	public R save(@Valid @RequestBody Oss oss) {
		CacheUtil.clear(RESOURCE_CACHE);
		return R.status(ossService.save(oss));
	}

	/**
	 * 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入oss")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	public R update(@Valid @RequestBody Oss oss) {
		boolean updated=ossService.updateById(oss);
		CacheUtil.clear(RESOURCE_CACHE);
		return R.status(updated);
	}

	/**
	 * 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入oss")
	@PreAuth("hasPermission('blade:oss:submit')")
	public R submit(@Valid @RequestBody Oss oss) {
		boolean updated=ossService.submit(oss);
		CacheUtil.clear(RESOURCE_CACHE);
		return R.status(updated);
	}


	/**
	 * 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	@PreAuth("hasPermission('blade:oss:remove')")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		CacheUtil.clear(RESOURCE_CACHE);
		return R.status(ossService.deleteLogic(Func.toLongList(ids)));
	}


	/**
	 * 启用
	 */
	@PostMapping("/enable")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "配置启用", description = "传入id")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	public R enable(@Parameter(description = "主键", required = true) @RequestParam Long id) {
		boolean updated=ossService.enable(id);
		CacheUtil.clear(RESOURCE_CACHE);
		return R.status(updated);
	}

	/**
	 * 判断是否是当前oss对应的文件
	 *
	 */
	@PostMapping("/get-curr-oss")
	@ApiOperationSupport(order = 1)
	public R getCurrOssDomain() {
		Oss oss = ossService.getCurrOss();
		return R.data(oss);
	}


}
