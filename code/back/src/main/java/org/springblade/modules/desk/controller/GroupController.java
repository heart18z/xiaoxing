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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.desk.entity.GroupEntity;
import org.springblade.modules.desk.service.IGroupService;
import org.springblade.modules.desk.vo.GroupUserVO;
import org.springblade.modules.desk.vo.GroupVO;
import org.springblade.modules.desk.wrapper.GroupWrapper;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 用户分组 控制器
 *
 * @author BladeX
 * @since 2023-08-07
 */
@RestController
@AllArgsConstructor
@RequestMapping("blade-group/group")
@Tag(name = "用户分组接口", description = "用户分组")
public class GroupController extends BladeController {

	private final IGroupService groupService;

	/**
	 * 用户分组 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入group")
	public R<GroupVO> detail(GroupEntity group) {
		GroupEntity detail = groupService.getOne(Condition.getQueryWrapper(group));
		return R.data(GroupWrapper.build().entityVO(detail));
	}
	/**
	 * 用户分组 树列表
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入group")
	public R<List<GroupVO>> list(@RequestParam Map<String, Object> group, BladeUser bladeUser) {
		QueryWrapper<GroupEntity> queryWrapper = Condition.getQueryWrapper(group, GroupEntity.class);
		List<GroupEntity> list = groupService.list((!bladeUser.getTenantId().equals(BladeConstant.ADMIN_TENANT_ID)) ? queryWrapper.lambda().eq(GroupEntity::getTenantId, bladeUser.getTenantId()) : queryWrapper);
		return R.data(GroupWrapper.build().treeNodeVO(list));
	}


   	/**
   	 * 懒加载列表
   	 */
   	@PostMapping("/lazy-list")
   	@ApiOperationSupport(order = 3)
   	@Operation(summary = "懒加载列表", description = "传入param")
   	public R<IPage<GroupVO>> lazyList(Query query, @RequestParam Map<String, Object> param) {
   		IPage<GroupVO> list = groupService.lazyList(Condition.getPage(query), param);
   		return R.data(list);
   	}



	/**
	 * 用户分组 自定义分页
	 */
	@PostMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入group")
	public R<IPage<GroupVO>> page(GroupVO group, Query query) {
		IPage<GroupVO> pages = groupService.selectGroupPage(Condition.getPage(query), group);
		return R.data(pages);
	}

	/**
	 * 用户分组 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入group")
	public R save(@Valid @RequestBody GroupEntity group) {
		return R.status(groupService.save(group));
	}

	/**
	 * 用户分组 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入group")
	public R update(@Valid @RequestBody GroupEntity group) {
		return R.status(groupService.updateById(group));
	}

	/**
	 * 用户分组 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入group")
	public R submit(@Valid @RequestBody GroupEntity group) {
		return R.status(groupService.saveOrUpdate(group));
	}

	/**
	 * 用户分组 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(groupService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * 用户分组 树形结构
	 */
	@PostMapping("/tree")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "树形结构", description = "树形结构")
	public R<List<GroupVO>> tree(String tenantId, BladeUser bladeUser) {
		List<GroupVO> tree = groupService.tree();
		return R.data(tree);
	}



	/**
	 * 获取用户数据
	 */
	@PostMapping("/group-user-list")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分组用户表", description = "传入param")
	public R<List<GroupUserVO>> getGroupUserList(Long groupId) {
		List<GroupUserVO> list = groupService.getGroupUserList(groupId);
		return R.data(list);
	}

	/**
	 * 用户分组 新增或修改
	 */
	@PostMapping("/group-user-submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入group")
	public R groupUserSubmit(@RequestBody GroupVO group) {
		return R.status(groupService.groupUserSubmit(group));
	}
}
