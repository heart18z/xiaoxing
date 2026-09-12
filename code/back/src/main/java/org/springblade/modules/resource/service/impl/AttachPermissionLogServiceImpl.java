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
package org.springblade.modules.resource.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import org.springblade.modules.resource.entity.AttachPermissionLogEntity;
import org.springblade.modules.resource.vo.AttachPermissionLogVO;
import org.springblade.modules.resource.mapper.AttachPermissionLogMapper;
import org.springblade.modules.resource.service.IAttachPermissionLogService;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.service.IUserService;
import org.springframework.stereotype.Service;
import org.springblade.core.tool.utils.Func;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件权限修改日志 服务实现类
 *
 * @author BladeX
 * @since 2024-08-20
 */
@Service
@AllArgsConstructor
public class AttachPermissionLogServiceImpl extends ServiceImpl<AttachPermissionLogMapper, AttachPermissionLogEntity> implements IAttachPermissionLogService {
	private final IUserService userService;

	@Override
	public boolean saveLog(List<Long>  sourceId) {
		Date date = new Date();
		Long userId = AuthUtil.getUserId();
		String account = AuthUtil.getUserAccount();
		List<AttachPermissionLogEntity> attachPermissionLogEntities = sourceId.stream().map(s->
			new AttachPermissionLogEntity(){{
				setAttachSourceId(s);
				setCreateBy(userId);
				setPermissionAccount(account);
				setCreateTime(date);
			}}).collect(Collectors.toList());
		if (!attachPermissionLogEntities.isEmpty()) {
			this.saveBatch(attachPermissionLogEntities);
		}
		return true;
	}

	@Override
	public List<AttachPermissionLogVO> listByAttachSource(Long attachSourceId) {
		if (attachSourceId==null) {
			return Collections.emptyList();
		}
		List<AttachPermissionLogEntity> entities = this.list(new LambdaQueryWrapper<AttachPermissionLogEntity>()
			.eq(AttachPermissionLogEntity::getAttachSourceId,attachSourceId)
			.orderByDesc(AttachPermissionLogEntity::getCreateTime)
		);
		List<User> users = this.userService.list();
		Map<String,String> userAccountNameMap = users.stream()
			.filter(i->Func.isNotBlank(i.getAccount())&&Func.isNotBlank(i.getRealName()))
			.collect(Collectors.toMap(User::getAccount,User::getRealName));

		Map<Long,String> userIdNameMap = users.stream()
			.filter(i->Func.isNotBlank(i.getAccount())&&Func.isNotBlank(i.getRealName()))
			.collect(Collectors.toMap(User::getId,i->i.getRealName()+"（"+i.getAccount()+"）"));

		List<AttachPermissionLogVO> res = entities.stream().map(i->{
			AttachPermissionLogVO vo = BeanUtil.copy(i,AttachPermissionLogVO.class);
			vo.setAccountName(toNameAccountStr(vo.getPermissionAccount(),userAccountNameMap));
			vo.setUserName(userIdNameMap.get(vo.getCreateBy()));
			return vo;
		}).collect(Collectors.toList());

		return res;
	}

	private String toNameAccountStr(String accounts,Map<String,String> userAccountNameMap ) {
		if (Func.isBlank(accounts)) {
			return "";
		}
		List<String> accountList = Func.toStrList(accounts);
		List<String> all = new ArrayList<>();
		for (String str : accountList) {
			if (Func.isBlank(str)) {continue;}
			String name = userAccountNameMap.get(str);
			if (Func.isNotBlank(name)) {
				all.add(name+"（"+str+"）");
			} else {
				all.add(str);
			}
		}
		return Func.join(all);

	}
}
