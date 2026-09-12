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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springblade.common.cache.DictCache;
import org.springblade.common.cache.UserCache;
import org.springblade.common.enums.DictEnum;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.cache.utils.CacheUtil;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.system.entity.Dept;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.service.IDeptService;
import org.springblade.modules.system.vo.DeptVO;
import org.springblade.modules.system.wrapper.DeptWrapper;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

import static org.springblade.core.cache.constant.CacheConstant.SYS_CACHE;

/**
 * 控制器
 *
 * @author Chill
 */
@NonDS
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_SYSTEM_NAME + "/dept")
@Tag(name = "部门", description = "部门")
public class DeptController extends BladeController {

	private final IDeptService deptService;

	/**
	 * 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入dept")
	@PreAuth("hasPermission('blade:dept:detail')")
	public R<DeptVO> detail(Dept dept) {
		Dept detail = deptService.getOne(Condition.getQueryWrapper(dept));
		return R.data(DeptWrapper.build().entityVO(detail));
	}

	/**
	 * 列表
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "列表", description = "传入dept")
	public R<List<DeptVO>> list(@RequestParam Map<String, Object> dept, BladeUser bladeUser) {
		QueryWrapper<Dept> queryWrapper = Condition.getQueryWrapper(dept, Dept.class);
		List<Dept> list = deptService.list((!bladeUser.getTenantId().equals(BladeConstant.ADMIN_TENANT_ID)) ? queryWrapper.lambda().eq(Dept::getTenantId, bladeUser.getTenantId()) : queryWrapper);
		return R.data(DeptWrapper.build().listNodeVO(list));
	}

	/**
	 * 懒加载列表
	 */
	@PostMapping("/lazy-list")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "懒加载列表", description = "传入dept")
	@PreAuth("hasPermission('blade:dept:lazy-list')")
	public R<List<DeptVO>> lazyList(@RequestParam Map<String, Object> dept, Long parentId, BladeUser bladeUser) {
		List<DeptVO> list = deptService.lazyList(bladeUser.getTenantId(), parentId, dept);
		return R.data(DeptWrapper.build().listNodeLazyVO(list));
	}

	/**
	 * 获取部门树形结构
	 */
	@PostMapping("/tree")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "树形结构", description = "树形结构")
	public R<List<DeptVO>> tree(String tenantId, BladeUser bladeUser) {
		List<DeptVO> tree = deptService.tree(Func.toStrWithEmpty(tenantId, bladeUser.getTenantId()));
		return R.data(tree);
	}

	/**
	 * 懒加载获取部门树形结构
	 */
	@PostMapping("/lazy-tree")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "懒加载树形结构", description = "树形结构")
	public R<List<DeptVO>> lazyTree(String tenantId, Long parentId, BladeUser bladeUser) {
		List<DeptVO> tree = deptService.lazyTree(Func.toStrWithEmpty(tenantId, bladeUser.getTenantId()), parentId);
		return R.data(tree);
	}

	/**
	 * 新增或修改
	 */
	@PreAuth("hasPermission('blade:dept:submit')")
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入dept")
	public R submit(@Valid @RequestBody Dept dept) {
		if (deptService.submit(dept)) {
			CacheUtil.clear(SYS_CACHE);
			CacheUtil.clear(SYS_CACHE, Boolean.FALSE);
			// 返回懒加载树更新节点所需字段
			Kv kv = Kv.create().set("id", String.valueOf(dept.getId())).set("tenantId", dept.getTenantId())
				.set("deptCategoryName", DictCache.getValue(DictEnum.ORG_CATEGORY, dept.getDeptCategory()));
			return R.data(kv);
		}
		return R.fail("操作失败");
	}

	/**
	 * 删除
	 */

	@PostMapping("/remove")
	@PreAuth("hasPermission('blade:dept:remove')")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		CacheUtil.clear(SYS_CACHE);
		CacheUtil.clear(SYS_CACHE, Boolean.FALSE);
		return R.status(deptService.removeDept(ids));
	}

	/**
	 * 下拉数据源
	 */
	@PostMapping("/select")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "下拉数据源", description = "传入id集合")
	public R<List<Dept>> select(Long userId, String deptId) {
		if (Func.isNotEmpty(userId)) {
			User user = UserCache.getUser(userId);
			deptId = user.getDeptId();
		}
		List<Dept> list = deptService.list(Wrappers.<Dept>lambdaQuery().in(Dept::getId, Func.toLongList(deptId)));
		return R.data(list);
	}


	/**
	 * 下拉数据源
	 */
	@PostMapping("/check-dept")
	public R<List<DeptVO>> check() {
		long count = deptService.count(new LambdaQueryWrapper<Dept>().eq(Dept::getDeptName,"外部机构"));
		if (count == 0) {
			deptService.save(new Dept(){{setDeptName("外部机构");setParentId(new Long(0));setAncestors("0");setDeptCategory(1);setFullName("外部机构");}});
		}
		List<DeptVO> tree = deptService.tree("000000");
		return R.data(tree);
	}


}
