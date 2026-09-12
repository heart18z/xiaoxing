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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import org.springblade.modules.quartz.constants.ScheduleConstants;
import org.springblade.modules.system.entity.BizParam;
import org.springblade.modules.system.service.IBizParamService;
import org.springblade.modules.system.vo.BizParamParam;
import org.springblade.modules.system.vo.BizParamVO;
import org.springblade.modules.system.wrapper.BizParamWrapper;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

import static org.springblade.core.cache.constant.CacheConstant.BIZ_CACHE;


/**
 * 系统参数表 控制器
 *
 * @author BladeX
 * @since 2022-03-14
 */
@NonDS
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_SYSTEM_NAME + "/bizParam")
@Tag(name = "业务参数表接口", description = "业务参数表")
public class BizParamController extends BladeController {

	private final IBizParamService bizParamService;

	/**
	 * 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入bizParam")
	public R<BizParamVO> detail(BizParam bizParam) {
		BizParamVO detail = bizParamService.getOne(bizParam);
		return R.data(BizParamWrapper.build().entityVO(detail));
	}

	/**
	 * 分页 系统参数表
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "列表", description = "传入bizParam")
	public R<List<BizParamVO>> list(@RequestParam Map<String, Object> bizParam) {
		List<BizParam> list = bizParamService.list(Condition.getQueryWrapper(bizParam, BizParam.class).lambda().orderByAsc(BizParam::getSort));
		return R.data(BizParamWrapper.build().listNodeVO(list));
	}

	/**
	 * 顶级列表
	 *
	 * @return
	 */
	@PostMapping("/parent-list")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "列表", description = "传入bizParam")
	@PreAuth("hasPermission('blade:bizParam:parent-list')")
	public R<IPage<BizParamVO>> parentList( BizParamVO bizParam, Query query) {
		return R.data(bizParamService.parentList(bizParam, query));
	}

	/**
	 * 子列表
	 */
	@PostMapping("/child-list")
	@PreAuth("hasPermission('blade:bizParam:child-list')")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "列表", description = "传入bizParam")
	public R<List<BizParamVO>> childList(@RequestParam Map<String, Object> bizParam, @RequestParam(required = false, defaultValue = "-1") Long parentId) {
		return R.data(bizParamService.childList(bizParam, parentId));
	}

	/**
	 * 获取参数树形结构
	 */
	@PostMapping("/tree")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "树形结构", description = "树形结构")
	public R<List<BizParamVO>> tree() {
		List<BizParamVO> tree = bizParamService.tree();
		return R.data(tree);
	}



	@PostMapping("/tree-by-path")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "参数路径树形结构", description = "根据参数路径，获取该路径下的树形参数")
	public R<? extends BizParam> treeByPath(@RequestParam(required = false) String path) {
		BizParamVO tree = bizParamService.treeByPath(path);
		return R.data(tree);
	}



	/**
	 * 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入bizParam")
	@PreAuth("hasPermission('blade:bizParam:submit')")
	public R submit(@Valid @RequestBody BizParam bizParam) {
		CacheUtil.clear(BIZ_CACHE, Boolean.FALSE);
		if (bizParam.getId() ==null) {
			bizParam.setIsSync(ScheduleConstants.DATA_SOURCE_ADD);
		}
		if (!bizParamService.getNoPermissionParams(bizParam)) {
			return R.fail("您无权限修改此参数");
		} else {
			// 新增或修改时同级别参数下是否重复
			long sameCount = bizParamService.count(Wrappers.<BizParam>lambdaQuery()
				.eq(BizParam::getParentId, bizParam.getParentId())
				.eq(BizParam::getParamValue, bizParam.getParamValue())
				.ne(bizParam.getId() != null, BizParam::getId, bizParam.getId()));
			if (sameCount > 0) {
				return R.fail("参数值重复！");
			}
			long sameNameCount = bizParamService.count(Wrappers.<BizParam>lambdaQuery()
				.eq(BizParam::getParentId, bizParam.getParentId())
				.eq(BizParam::getParamName, bizParam.getParamName())

				.ne(bizParam.getId() != null, BizParam::getId, bizParam.getId()));
			if (sameNameCount > 0) {
				return R.fail("同级参数名称重复！");
			}
		}
		return R.data(bizParamService.submit(bizParam));
	}


	/**
	 * 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "删除", description = "传入ids")
	@PreAuth("hasPermission('blade:bizParam:remove')")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		CacheUtil.clear(BIZ_CACHE, Boolean.FALSE);
		Collection<BizParam> noPermissionParams = bizParamService.getNoPermissionParams(ids, true);
		if (noPermissionParams.size() > 0) {
			// 当前用户不是管理员，不可删除
			String rs = noPermissionParams.stream()
				.map(BizParam::getParamName)
				.collect(Collectors.joining(","));
			return R.fail("您无权限删除参数：" + rs);
		} else {
			// 判断是否包含子级
			long childrenCount = bizParamService.count(Wrappers.<BizParam>lambdaQuery().in(BizParam::getParentId, Arrays.asList(ids.split(","))));
			if (childrenCount > 0) {
				return R.fail("请先删除所有的子级参数");
			}
		}
		return R.status(bizParamService.removeBizParam(ids));
	}



	/**
	 *
	 * @param bizParamParam paramValue 是参数值
	 *                      isTree 是否以树形结构的形式返回数据，默认不按照树形结构返回
	 * @return
	 */
	@PostMapping("/dictionary")
	@ApiOperationSupport(order = 11)
	@Operation(summary = "根据上级的参数值获取下级参数列表", description = "根据父id获取参数")
	public R<Object> dictionary (BizParamParam bizParamParam) {

		List<BizParamVO> list= bizParamService.dictionary(bizParamParam);
		return  R.data(list);
	}





}
