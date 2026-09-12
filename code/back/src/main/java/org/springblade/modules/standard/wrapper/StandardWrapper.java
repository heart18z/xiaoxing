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
package org.springblade.modules.standard.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.modules.standard.entity.StandardEntity;
import org.springblade.modules.standard.vo.StandardVO;

import java.util.Objects;

/**
 * 规范类列表 包装类,返回视图层所需的字段
 *
 * @author linchaofan
 * @since 2023-06-09
 */
public class StandardWrapper extends BaseEntityWrapper<StandardEntity, StandardVO>  {

	public static StandardWrapper build() {
		return new StandardWrapper();
 	}

	@Override
	public StandardVO entityVO(StandardEntity standard) {
		StandardVO standardVO = Objects.requireNonNull(BeanUtil.copy(standard, StandardVO.class));

		//User createUser = UserCache.getUser(standard.getCreateUser());
		//User updateUser = UserCache.getUser(standard.getUpdateUser());
		//standardVO.setCreateUserName(createUser.getName());
		//standardVO.setUpdateUserName(updateUser.getName());

		return standardVO;
	}

//	public List<StandardVO> listNodeLazyVO(List<StandardVO> list) {
//		return ForestNodeMerger.merge(list);
//	}


}
