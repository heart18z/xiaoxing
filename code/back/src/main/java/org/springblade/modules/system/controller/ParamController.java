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
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springblade.common.cache.ParamCache;
import org.springblade.common.cache.UserCache;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.cache.utils.CacheUtil;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.system.entity.Param;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.service.IParamService;
import org.springblade.modules.system.vo.ParamVO;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springblade.core.cache.constant.CacheConstant.PARAM_CACHE;

/**
 * 控制器
 *
 * @author Chill
 */
@NonDS
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_SYSTEM_NAME + "/param")
@Tag(name = "参数配置", description = "参数配置")
public class ParamController extends BladeController {

	private final IParamService paramService;

	/**
	 * 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入param")
	@PreAuth("hasPermission('blade:param:detail')")
	public R<ParamVO> detail(Param param) {
		Param detail = paramService.getOne(Condition.getQueryWrapper(param));
		if (detail != null) {
			ParamVO paramVO = BeanUtil.copy(detail, ParamVO.class);
			User updateUser = UserCache.getUser(detail.getUpdateUser());
			paramVO.setUpdateUserName(Func.notNull(updateUser)?updateUser.getRealName():null);
			return R.data(paramVO);
		} else {
			return R.data(null);
		}
	}

	/**
	 * 详情
	 */
	@PostMapping("/get-value-by-key")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入param")
	public R<String> detail(String  key) {
		return R.data(ParamCache.getValue(key));
	}

	/**
	 * 分页
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入param")
	@PreAuth("hasPermission('blade:param:list')")
	public R<IPage<Param>> list(@RequestParam Map<String, Object> param, Query query) {
		IPage<Param> pages = paramService.page(Condition.getPage(query), Condition.getQueryWrapper(param, Param.class));
		return R.data(pages);
	}

	/**
	 * 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "新增或修改", description = "传入param")
	@PreAuth("hasPermission('blade:param:submit')")
	public R submit(@Valid @RequestBody Param param) {
		CacheUtil.clear(PARAM_CACHE);
		CacheUtil.clear(PARAM_CACHE, Boolean.FALSE);

		return R.status(paramService.saveOrUpdateParam(param));
	}



	/**
	 * 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "逻辑删除", description = "传入ids")
	@PreAuth("hasPermission('blade:param:remove')")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		CacheUtil.clear(PARAM_CACHE);
		CacheUtil.clear(PARAM_CACHE, Boolean.FALSE);
		return R.status(paramService.removeParam(Func.toLongList(ids)));
	}

	/**
	 * 获取参数配置map
	 *
	 * @return Map
	 */
	@PostMapping("/param-map")
	public  R<Map> getKeyValueMap() {
		List<Param> paramList =paramService.list(new QueryWrapper<>());
		Map<String ,Object> mapResult = paramList.stream().collect(Collectors.toMap(Param::getParamKey,Param::getParamValue,(v1,v2)->v1));
		Map<String ,String> mapKeyName = paramList.stream().collect(Collectors.toMap(Param::getParamKey,Param::getParamName,(v1,v2)->v1));
		mapResult.put("mapKeyName",mapKeyName);
		return R.data(mapResult);
	}

	/**
	 * 获取首页标题，以及页签title,以及是否有验证码
	 */
	@PostMapping("/login-page-param")
	public  R<Map> getLoginPageParam() {
		LambdaQueryWrapper<Param> lambdaQueryWrapper = new LambdaQueryWrapper<Param>()
			.in(Param::getParamKey,new String[]{"chekcode","invitation.code","project.name",
				"admin.passwd","account.initPassword","adminer.adm.produ","system.id","oauth2","showRememberMe"});
		return R.data(paramService.list(lambdaQueryWrapper).stream().collect(Collectors.toMap(Param::getParamKey,Param::getParamValue,(v1, v2) -> v1)));
	}

}
