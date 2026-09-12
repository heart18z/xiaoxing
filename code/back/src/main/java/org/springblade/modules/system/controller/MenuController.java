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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.cache.utils.CacheUtil;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.node.TreeNode;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.system.entity.Menu;
import org.springblade.modules.system.entity.TopMenu;
import org.springblade.modules.system.service.IMenuService;
import org.springblade.modules.system.service.ITopMenuService;
import org.springblade.modules.system.vo.CheckedTreeVO;
import org.springblade.modules.system.vo.GrantTreeVO;
import org.springblade.modules.system.vo.MenuVO;
import org.springblade.modules.system.wrapper.MenuWrapper;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

import static org.springblade.core.cache.constant.CacheConstant.MENU_CACHE;

/**
 * 控制器
 *
 * @author Chill
 */
@NonDS
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_SYSTEM_NAME + "/menu")
@Tag(name = "菜单", description = "菜单")
public class MenuController extends BladeController {

	private final IMenuService menuService;
	private final ITopMenuService topMenuService;

	/**
	 * 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入menu")
	@PreAuth("hasPermission('blade:menu:detail')")
	public R<MenuVO> detail(Menu menu) {
		Menu detail = menuService.getOne(Condition.getQueryWrapper(menu));
		return R.data(MenuWrapper.build().entityVO(detail));
	}

	/**
	 * 列表
	 */
	@PostMapping("/list")
	@PreAuth(RoleConstant.HAS_ROLE_ADMINISTRATOR)
	@ApiOperationSupport(order = 2)
	@Operation(summary = "列表", description = "传入menu")
	public R<List<MenuVO>> list(@RequestParam Map<String, Object> menu) {
		List<Menu> list = menuService.list(Condition.getQueryWrapper(menu, Menu.class).lambda().orderByAsc(Menu::getSort));
		return R.data(MenuWrapper.build().listNodeVO(list));
	}

	/**
	 * 懒加载列表
	 */
	@PostMapping("/lazy-list")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "懒加载列表", description = "传入menu")
	@PreAuth("hasPermission('blade:menu:lazy-list')")

	public R<List<MenuVO>> lazyList(Long parentId, @RequestParam Map<String, Object> menu) {
		List<MenuVO> list = menuService.lazyList(parentId, menu);
		return R.data(MenuWrapper.build().listNodeLazyVO(list));
	}

	/**
	 * 菜单列表
	 */
	@PostMapping("/menu-list")
	@PreAuth(RoleConstant.HAS_ROLE_ADMINISTRATOR)
	@ApiOperationSupport(order = 4)
	@Operation(summary = "菜单列表", description = "传入menu")
	public R<List<MenuVO>> menuList(@RequestParam Map<String, Object> menu) {
		List<Menu> list = menuService.list(Condition.getQueryWrapper(menu, Menu.class).lambda().eq(Menu::getCategory, 1).orderByAsc(Menu::getSort));
		return R.data(MenuWrapper.build().listNodeVO(list));
	}

	/**
	 * 懒加载菜单列表
	 */
	@PostMapping("/lazy-menu-list")
	@PreAuth(RoleConstant.HAS_ROLE_ADMINISTRATOR)
	@ApiOperationSupport(order = 5)
	@Operation(summary = "懒加载菜单列表", description = "传入menu")
	public R<List<MenuVO>> lazyMenuList(Long parentId, @RequestParam Map<String, Object> menu) {
		List<MenuVO> list = menuService.lazyMenuList(parentId, menu);
		return R.data(MenuWrapper.build().listNodeLazyVO(list));
	}

	/**
	 * 新增或修改
	 */
	@PostMapping("/submit")
	@PreAuth("hasPermission('blade:menu:submit')")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入menu")
	public R submit(@Valid @RequestBody Menu menu) {
		if (menuService.submit(menu)) {
			CacheUtil.clear(MENU_CACHE);
			CacheUtil.clear(MENU_CACHE, Boolean.FALSE);
			// 返回懒加载树更新节点所需字段
			Kv kv = Kv.create().set("id", String.valueOf(menu.getId()));
			return R.data(kv);
		}
		return R.fail("操作失败");
	}


	/**
	 * 删除
	 */
	@PostMapping("/remove")
	@PreAuth("hasPermission('blade:menu:remove')")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		CacheUtil.clear(MENU_CACHE);
		CacheUtil.clear(MENU_CACHE, Boolean.FALSE);
		return R.status(menuService.removeMenu(ids));
	}

	/**
	 * 前端菜单数据
	 */
	@PostMapping("/routes")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "前端菜单数据", description = "前端菜单数据")
	public R<List<MenuVO>> routes(BladeUser user, Long topMenuId) {
		List<MenuVO> list = menuService.routes((user == null) ? null : user.getRoleId(), topMenuId);
		return R.data(list);
	}

	/**
	 * 前端菜单数据
	 */
	@PostMapping("/routes-ext")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "前端菜单数据", description = "前端菜单数据")
	public R<List<MenuVO>> routesExt(BladeUser user, Long topMenuId) {
		List<MenuVO> list = menuService.routesExt(user.getRoleId(), topMenuId);
		return R.data(list);
	}

	/**
	 * 前端按钮数据
	 */
	@PostMapping("/buttons")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "前端按钮数据", description = "前端按钮数据")
	public R<List<MenuVO>> buttons(BladeUser user) {
		List<MenuVO> list = menuService.buttons(user.getRoleId());
		return R.data(list);
	}

	/**
	 * 获取菜单树形结构
	 */
	@PostMapping("/tree")
	@ApiOperationSupport(order = 11)
	@Operation(summary = "树形结构", description = "树形结构")
	public R<List<TreeNode>> tree() {
		List<TreeNode> tree = menuService.tree();
		return R.data(tree);
	}

	/**
	 * 获取权限分配树形结构
	 */
	@PostMapping("/grant-tree")
	@ApiOperationSupport(order = 12)
	@Operation(summary = "权限分配树形结构", description = "权限分配树形结构")
	public R<GrantTreeVO> grantTree(BladeUser user) {
		GrantTreeVO vo = new GrantTreeVO();
		vo.setMenuVOS(menuService.grantMenuVOTree(user));
		vo.setDataScope(menuService.grantDataScopeTree(user));
		vo.setApiScope(menuService.grantApiScopeTree(user));
		return R.data(vo);
	}

	/**
	 * 获取权限分配树形结构
	 */
	@PostMapping("/role-tree-keys")
	@ApiOperationSupport(order = 13)
	@Operation(summary = "角色所分配的树", description = "角色所分配的树")
	public R<CheckedTreeVO> roleTreeKeys(String roleIds) {
		CheckedTreeVO vo = new CheckedTreeVO();
		vo.setMenu(menuService.roleTreeKeys(roleIds));
		vo.setDataScope(menuService.dataScopeTreeKeys(roleIds));
		vo.setApiScope(menuService.apiScopeTreeKeys(roleIds));
		return R.data(vo);
	}

	/**
	 * 获取顶部菜单树形结构
	 */
	@PostMapping("/grant-top-tree")
	@ApiOperationSupport(order = 14)
	@Operation(summary = "顶部菜单树形结构", description = "顶部菜单树形结构")
	public R<GrantTreeVO> grantTopTree(BladeUser user) {
		GrantTreeVO vo = new GrantTreeVO();
		vo.setMenu(menuService.grantTopTree(user));
		return R.data(vo);
	}

	/**
	 * 获取顶部菜单树形结构
	 */
	@PostMapping("/top-tree-keys")
	@ApiOperationSupport(order = 15)
	@Operation(summary = "顶部菜单所分配的树", description = "顶部菜单所分配的树")
	public R<CheckedTreeVO> topTreeKeys(String topMenuIds) {
		CheckedTreeVO vo = new CheckedTreeVO();
		vo.setMenu(menuService.topTreeKeys(topMenuIds));
		return R.data(vo);
	}

	/**
	 * 顶部菜单数据
	 */
	@PostMapping("/top-menu")
	@ApiOperationSupport(order = 16)
	@Operation(summary = "顶部菜单数据", description = "顶部菜单数据")
	public R<List<TopMenu>> topMenu(BladeUser user) {
		if (Func.isEmpty(user)) {
			return null;
		}
		List<TopMenu> list = topMenuService.list(Wrappers.<TopMenu>query().lambda().orderByAsc(TopMenu::getSort));
		return R.data(list);
	}

	/**
	 * 获取配置的角色权限
	 */
	@PostMapping("auth-routes")
	@ApiOperationSupport(order = 17)
	@Operation(summary = "菜单的角色权限")
	public R<List<Kv>> authRoutes(BladeUser user) {
		if (Func.isEmpty(user)) {
			return null;
		}
		return R.data(menuService.authRoutes(user));
	}

}
