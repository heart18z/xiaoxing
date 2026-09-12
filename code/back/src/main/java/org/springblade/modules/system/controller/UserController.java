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
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springblade.core.cache.utils.CacheUtil;

import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.redis.cache.BladeRedis;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringPool;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.standard.support.vo.KeyValueVO;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.service.IUserService;
import org.springblade.modules.system.vo.ContactUserVO;
import org.springblade.modules.system.vo.UserPlatformVO;
import org.springblade.modules.system.vo.UserVO;
import org.springblade.modules.system.wrapper.UserWrapper;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springblade.core.cache.constant.CacheConstant.USER_CACHE;

/**
 * 控制器
 *
 * @author Chill
 */
@NonDS
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_SYSTEM_NAME + "/user")
@Tag(name = "用户", description = "用户")
public class UserController {

	private final IUserService userService;
	private final BladeRedis bladeRedis;

	/**
	 * 查询单条
	 */
	@ApiOperationSupport(order = 1)
	@Operation(summary = "查看详情", description = "传入id")
	@PostMapping("/detail")
	@PreAuth("hasPermission('blade:user:detail')")
	public R<UserVO> detail(User user) {
		User detail = userService.getOne(Condition.getQueryWrapper(user));
		return R.data(UserWrapper.build().entityVO(detail));
	}

	/**
	 * 查询单条
	 */
	@ApiOperationSupport(order = 2)
	@Operation(summary = "查看详情", description = "传入id")
	@PostMapping("/info")
	public R<UserVO> info(BladeUser user) {
		User detail = userService.getById(user.getUserId());
		return R.data(UserWrapper.build().entityVO(detail));
	}

	/**
	 * 用户列表
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "列表", description = "传入account和realName")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	public R<IPage<UserVO>> list(@RequestParam Map<String, Object> user, Query query, BladeUser bladeUser) {
		QueryWrapper<User> queryWrapper = Condition.getQueryWrapper(user, User.class);
		IPage<User> pages = userService.page(Condition.getPage(query), (!bladeUser.getTenantId().equals(BladeConstant.ADMIN_TENANT_ID)) ?
			queryWrapper.lambda().eq(User::getTenantId, bladeUser.getTenantId()) : queryWrapper);
		return R.data(UserWrapper.build().pageVO(pages));
	}

	/**
	 * 自定义用户列表
	 */
	@PostMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "列表", description = "传入account和realName")
	@PreAuth("hasPermission('blade:user:page')")
	public R<IPage<UserVO>> page(User user, Query query, Long deptId,Long menuId, BladeUser bladeUser) {
		IPage<User> pages = userService.selectUserPage(Condition.getPage(query), user, deptId, menuId,
			(bladeUser.getTenantId().equals(BladeConstant.ADMIN_TENANT_ID) ? StringPool.EMPTY : bladeUser.getTenantId()));
		return R.data(UserWrapper.build().pageVO(pages));
	}

	/**
	 * 新增或修改
	 */

	@PostMapping("/submit")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增或修改", description = "传入User")
	@PreAuth("hasPermission('blade:user:submit')")
	public R submit(@Valid @RequestBody User user) {
		CacheUtil.clear(USER_CACHE);
		return R.status(userService.submit(user));
	}

	/**
	 * 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入User")
	@PreAuth("hasPermission('blade:user:update')")
	public R update(@Valid @RequestBody User user) {
		CacheUtil.clear(USER_CACHE);
		return R.status(userService.updateUser(user));
	}

	/**
	 * 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "删除", description = "传入id集合")
//	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)blade:user:remove
	@PreAuth("hasPermission('blade:user:remove')")
	public R remove(@RequestParam String ids) {
		CacheUtil.clear(USER_CACHE);
		return R.status(userService.removeUser(ids));
	}

	/**
	 * 设置菜单权限
	 */
	@PostMapping("/grant")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "权限设置", description = "传入roleId集合以及menuId集合")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	public R grant(@Parameter(description = "userId集合", required = true) @RequestParam String userIds,
				   @Parameter(description = "roleId集合", required = true) @RequestParam String roleIds) {
		boolean temp = userService.grant(userIds, roleIds);
		return R.status(temp);
	}

	/**
	 * 重置密码
	 */
	@PostMapping("/reset-password")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "初始化密码", description = "传入userId集合")
	//@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	@PreAuth("hasPermission('blade:user:reset-password')")
	public R resetPassword(@Parameter(description = "userId集合", required = true) @RequestParam String userIds) {
		boolean temp = userService.resetPassword(userIds);
		return R.status(temp);
	}

	/**
	 * 重置密码
	 */
	@PostMapping("/reset-password-show")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "初始化密码", description = "传入userId")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	public R resetPasswordShow(@Parameter(description = "userId", required = true) @RequestParam String userId) {
		String temp = userService.resetPasswordShow(userId);
		return R.data(temp);
	}

	/**
	 * 修改密码
	 */
	@PostMapping("/update-password")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "修改密码", description = "传入密码")
	public R updatePassword(BladeUser user, @Parameter(description = "旧密码", required = true) @RequestParam String oldPassword,
							@Parameter(description = "新密码", required = true) @RequestParam String newPassword,
							@Parameter(description = "新密码", required = true) @RequestParam String newPassword1) {
		boolean temp = userService.updatePassword(user.getUserId(), oldPassword, newPassword, newPassword1);
		return R.status(temp);
	}

	/**
	 * 修改基本信息
	 */
	@PostMapping("/update-info")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "修改基本信息", description = "传入User")
	public R updateInfo(@Valid @RequestBody User user) {
		CacheUtil.clear(USER_CACHE);
		return R.status(userService.updateUserInfo(user));
	}

	/**
	 * 用户列表
	 */
	@PostMapping("/user-list")
	@ApiOperationSupport(order = 11)
	@Operation(summary = "用户列表", description = "传入user")
	public R<List<User>> userList(User user, BladeUser bladeUser) {
		QueryWrapper<User> queryWrapper = Condition.getQueryWrapper(user);
		List<User> list = userService.list((!AuthUtil.isAdministrator()) ? queryWrapper.lambda().eq(User::getTenantId, bladeUser.getTenantId()) : queryWrapper);
		return R.data(list);
	}




	/**
	 * 第三方注册用户
	 */
	@PostMapping("/register-guest")
	@ApiOperationSupport(order = 15)
	@Operation(summary = "第三方注册用户", description = "传入user")
	public R registerGuest(User user, Long oauthId) {
		return R.status(userService.registerGuest(user, oauthId));
	}

	/**
	 * 配置用户平台信息
	 */
	@PostMapping("/update-platform")
	@ApiOperationSupport(order = 16)
	@Operation(summary = "配置用户平台信息", description = "传入user")
	public R updatePlatform(@RequestBody UserPlatformVO userPlatformVOS) {
		return R.status(userService.updatePlatform(userPlatformVOS.getUserPlatforms()));
	}

	/**
	 * 查看平台详情
	 */
	@ApiOperationSupport(order = 17)
	@Operation(summary = "查看平台详情", description = "传入id")
	@PostMapping("/platform-detail")
	@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
	public R<UserVO> platformDetail(User user) {
		return R.data(userService.platformDetail(user));
	}

	/**
	 * 用户解锁
	 */
	@PostMapping("/unlock")
	@ApiOperationSupport(order = 18)
	@Operation(summary = "账号解锁", description = "传入id")
	public R unlock(String userIds) {
		if (StringUtil.isBlank(userIds)) {
			return R.fail("请至少选择一个用户");
		}
		List<Long> idList =  Func.toLongList(userIds);
		if (idList.size()>0) {
			List<User> users = idList.stream().map(id->new User(){{setId(id);setStatus(1);}}).collect(Collectors.toList());
			userService.updateBatchById(users);
		}
		return R.success("操作成功");
	}

	/**
	 * 用户解锁
	 */
	@PostMapping("/lock")
	@ApiOperationSupport(order = 18)
	@Operation(summary = "账号锁定", description = "传入id")
	public R lock(String userIds) {
		if (StringUtil.isBlank(userIds)) {
			return R.fail("请至少选择一个用户");
		}
		List<Long> idList =  Func.toLongList(userIds);
		if (idList.size()>0) {
			List<User> users = idList.stream().map(id->new User(){{
				setId(id);setStatus(2);
				setIdentity("");
			}}).collect(Collectors.toList());
			userService.updateBatchById(users);
		}
		return R.success("操作成功");
	}

	/**
	 * 获取首页用户联系方式
	 */
	@PostMapping("/user-contact")
	@ApiOperationSupport(order = 19)
	@Operation(summary = "列表")
	public R<ContactUserVO> userContact(boolean isMain) {
		return R.data(userService.getUserContact(isMain));
	}


	/**
	 * 获取用户姓名和id的对应kv列表
	 */
	@PostMapping("/user-kv-list")
	@ApiOperationSupport(order = 20)
	@Operation(summary = "列表")
	public R<List<KeyValueVO>> userKvList() {
		return R.data(userService.getUserKvList());
	}


	/**
	 * 获取用户姓名和账号的对应kv列表
	 */
	@PostMapping("/user-account-kv-list")
	@ApiOperationSupport(order = 20)
	@Operation(summary = "列表")
	public R<List<KeyValueVO>> userAccountKvList() {
		return R.data(userService.getUserAccountKvList());
	}

	/**
	 * 获取admin用户列表
	 */
	@PostMapping("/user-contact-dict")
	@ApiOperationSupport(order = 19)
	@Operation(summary = "列表")
	public R<List<KeyValueVO>> dict() {
		List<KeyValueVO> list = userService.getUserContactKeyValue();
		return R.data(list);
	}

}
