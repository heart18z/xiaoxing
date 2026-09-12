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
package org.springblade.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.AllArgsConstructor;
import org.springblade.common.cache.ParamCache;
import org.springblade.common.constant.CommonConstant;
import org.springblade.common.constant.ParamCacheConstant;
import org.springblade.core.cache.constant.CacheConstant;
import org.springblade.core.cache.utils.CacheUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.desk.enums.BizScheduledTriggerModeEnum;
import org.springblade.modules.quartz.executor.BaseDictExecutor;
import org.springblade.modules.system.entity.*;
import org.springblade.modules.system.mapper.ParamMapper;
import org.springblade.modules.system.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.springblade.common.constant.TenantConstant.DEFAULT_PASSWORD;
import static org.springblade.common.constant.TenantConstant.PASSWORD_KEY;

/**
 * 服务实现类
 *
 * @author Chill
 */
@Service
@AllArgsConstructor
public class ParamServiceImpl extends BaseServiceImpl<ParamMapper, Param> implements IParamService {

	private final BaseDictExecutor baseDictExecutor;

	private final IInteractiveService interactiveService;

	private final IBizParamService bizParamService;

	private final RemoteParamService remoteParamService;

	private final IRoleService roleService;

	private final IUserService userService;

	private final IRoleMenuService roleMenuService;
	private final IMenuService menuService;

	@Override
	public String getValue(String paramKey) {
		Param param = this.getOne(Wrappers.<Param>query().lambda().eq(Param::getParamKey, paramKey).last("limit 1"));
		return param==null?null:param.getParamValue();
	}

	/**
	 * 启用远程字典时，当前本地应用字典中的手动创建数据（含同步的链状父级）将被保留，其余同步数据将被清空，
	 * 同时所有本地应用字典状态将被更改为“本地创建”；
	 * 1、如果仍然需要在远程字典中使用本地应用字典的数据，请在远程字典系统中，手动完成创建，
	 * 2、保留在本地应用的手动创建的数据（含同步的链状父级），在开启远程字典后，将不会被系统调用；
	 * 关闭远程字典时，系统将会按如下顺序执行：
	 * 1、删除当前本地应用字典中的所有数据；
	 * 2、将远程字典中的“标准字典数据”和专门为本系统创建的“个性字典数据”全部同步到本系统的应用字典中，并全部标记为“数据同步”
	 * 3、系统将在本系统的“上线设定-对外交互”菜单中，自动调整“远程调用基础字典”的状态为禁用；
	 * @param param
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean saveOrUpdateParam(Param param) {
		if (Func.isBlank(param.getParamKey())) {
			throw new ServiceException("参数键名不能为空！");
		}
		LambdaQueryWrapper<Param> lambdaQueryWrapper = new LambdaQueryWrapper();
		lambdaQueryWrapper.eq(Param::getParamKey,param.getParamKey());
		if (param.getId() != null) {
			lambdaQueryWrapper.ne(Param::getId,param.getId());
		}
		long count = this.count(lambdaQueryWrapper);
		if (count>0) {
			throw new ServiceException("参数键名不能重复！");
		}

		if (param.getId()!=null&&CommonConstant.REMOTE_BASE_DICT_PARAM_KEY.equals(param.getParamKey())) {
			Param dbParam = this.getById(param.getId());
			if (Func.isNotBlank(dbParam.getParamValue())&&
				dbParam.getParamValue().equals("true")&&
				!"true".equals(param.getParamValue()))
			{
				remoteParamService.cleanAllSyncData();
				interactiveService.updateById(new InteractiveEntity(){{
					setId(CommonConstant.BASE_DICT_INTERACTIVE_ID);
					setStatus(CommonConstant.DB_STATUS_NOT_NORMAL);
				}});
				baseDictExecutor.doSyncBizParamByInterface(BizScheduledTriggerModeEnum.TASK_SYNC_HAND.getType());
			} else if (Func.isNotBlank(dbParam.getParamValue())&&
				param.getParamValue().equals("true")&&
				!"true".equals(dbParam.getParamValue())){
				try {
					remoteParamService.doSyncAiParam(false);
				} catch (Exception e) {
					e.printStackTrace();
				}

				InteractiveEntity interactiveEntity =interactiveService.getById(CommonConstant.BASE_DICT_INTERACTIVE_ID);
				Boolean interactiveOpen = false;
				if (Func.notNull(interactiveEntity)) {
					interactiveOpen = interactiveEntity.getStatus() == CommonConstant.DB_STATUS_NORMAL;
				}
				if (!interactiveOpen) {
					throw new ServiceException("请在本系统“上线设定-对外交互”中，将数据“远程调用基础字典”的状态变更后才能成功开启远程字典的调用。");
				}
				bizParamService.cleanAllSyncData();
			}

		}

		if (param.getId()!=null&& ParamCacheConstant.CREATE_APPLY_USER.equals(param.getParamKey())) {
			Param dbParam = this.getById(param.getId());
			if (Func.isNotBlank(dbParam.getParamValue())&&
				dbParam.getParamValue().equals("true")&&
				!"true".equals(param.getRemark()))
			{
				// 修改为 false
				delApplyUser(param.getRemark());
			}else if (Func.isNotBlank(dbParam.getParamValue())&&
				param.getParamValue().equals("true")&&
				!"true".equals(dbParam.getParamValue())){
				// 修改为 true
				createApplyUser(param.getRemark());
			}
		}


		this.saveOrUpdate(param);
		CacheUtil.clear(CacheConstant.PARAM_CACHE);
		return true;
	}

	private void createApplyUser(String value) {
		//创建所有人员 应用管理员(apply:apply_user)、安全管理员(safety:safety_user)、日志审计员(logAudit:log_audit_user)
		String[] users =  Func.split(value,"、");
		Date now = new Date();
		Arrays.asList(users).stream().forEach(i->{

			String realName = Func.split(i, "(")[0];
			String roleName = Func.split(Func.split(i, "(")[1],":")[0];
			String account = Func.split(Func.split(i, "(")[1],":")[1]
				.replace(")","");

			Role role = new Role();
			role.setRoleName(roleName);
			role.setRoleAlias(roleName);
			roleService.save(role);


			User user = new User();
			user.setRealName(realName);
			user.setUserType("1");
			user.setStatus(1);
			user.setAccount(account);
			user.setName(realName);
			user.setRoleId(role.getId().toString());
			user.setLastChangePasswordTime(now);
			user.setCreateTime(now);
			user.setUpdateTime(now);
			user.setLoginType("user_loginMethod_account");
			String password = Func.toStr(ParamCache.getValue(CommonConstant.DEFAULT_PARAM_PASSWORD), DEFAULT_PASSWORD);
			user.setPassword(password);
			userService.submit(user);

			List<Menu> menus = menuService.list(new LambdaQueryWrapper<Menu>().in(Menu::getCode,
				new String[]{"monitor","log","log_usual","log_api","log_error","attachRecovery","system"}));

			List<RoleMenu> menuRoles= menus.stream().map(m->{
				return new RoleMenu(){{
					setMenuId(m.getId());
					setRoleId(role.getId());
				}};
			}).collect(Collectors.toList());
			if (!Func.isEmpty(menuRoles)) {
				roleMenuService.saveBatch(menuRoles);
			}



		});

	}


	private void delApplyUser(String value) {
		//创建所有人员 应用管理员(apply:apply_user)、安全管理员(safety:safety_user)、日志审计员(logAudit:log_audit_user)
		String[] users =  Func.split(value,"、");

		Arrays.asList(users).stream().forEach(i->{

			String realName = Func.split(i, "(")[0];
			String roleName = Func.split(Func.split(i, "(")[1],":")[0];
			String account = Func.split(Func.split(i, "(")[1],":")[1]
				.replace(")","");


			roleService.remove(new LambdaQueryWrapper<Role>().eq(Role::getRoleAlias,roleName));
			User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getAccount,account));
			userService.remove(new LambdaQueryWrapper<User>().eq(User::getAccount,account));
			if (Func.notNull(user)) {
				roleMenuService.remove(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId,user.getRoleId()));
			}




		});

	}

	@Override
	public Boolean removeParam(List<Long> toLongList) {
		return deleteLogic(toLongList);
	}

}
