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
package org.springblade.modules.system.wrapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springblade.common.cache.DictCache;
import org.springblade.common.cache.SysCache;
import org.springblade.common.enums.DictEnum;
import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.system.entity.Tenant;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.vo.UserVO;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 包装类,返回视图层所需的字段
 *
 * @author Chill
 */
public class UserWrapper extends BaseEntityWrapper<User, UserVO> {

	public static UserWrapper build() {
		return new UserWrapper();
	}

	@Override
	public UserVO entityVO(User user) {
		UserVO userVO = Objects.requireNonNull(BeanUtil.copy(user, UserVO.class));
		Tenant tenant = SysCache.getTenant(user.getTenantId());
		List<String> roleName = SysCache.getRoleNames(user.getRoleId());
		List<String> deptName = SysCache.getDeptNames(user.getDeptId());
		List<String> postName = SysCache.getPostNames(user.getPostId());
		if (Func.isNotEmpty(user.getUserType())) {
			String[] types =  Func.split(user.getUserType(),",");
			for (int i = 0; i < types.length; i++) {
				types[i] = DictCache.getValue(DictEnum.USER_TYPE, types[i]);
			}
			userVO.setUserTypeName(Func.join(types));
		}
		userVO.setTenantName(tenant.getTenantName());
		userVO.setRoleName(Func.join(roleName));
		userVO.setDeptName(Func.join(deptName));
		userVO.setPostName(Func.join(postName));
		userVO.setSexName(DictCache.getValue(DictEnum.SEX, user.getSex()));

		return userVO;
	}
	@Override
	public IPage<UserVO> pageVO(IPage<User> pages) {
		List<UserVO> records = this.listVO(pages.getRecords());
		IPage<UserVO> pageVo = new Page(pages.getCurrent(), pages.getSize(), pages.getTotal());
		pageVo.setRecords(records);
		return pageVo;
	}
	@Override
	public List<UserVO> listVO(List<User> list) {
		return (List)list.stream().map(this::simpleEntityVO).collect(Collectors.toList());
	}



	public UserVO simpleEntityVO(User user) {
		UserVO userVO = Objects.requireNonNull(BeanUtil.copy(user, UserVO.class));
		List<String> roleName = SysCache.getRoleNames(user.getRoleId());
//		if (Func.isNotEmpty(user.getUserType())) {
//			String[] types =  Func.split(user.getUserType(),",");
//			for (int i = 0; i < types.length; i++) {
//				types[i] = DictCache.getValue(DictEnum.USER_TYPE, types[i]);
//			}
//			userVO.setUserTypeName(Func.join(types));
//		}
		userVO.setRoleName(Func.join(roleName));
		return userVO;
	}

}
