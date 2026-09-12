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

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.modules.system.entity.OptimizeEntity;
import org.springblade.modules.system.vo.OptimizeVO;

import java.util.Objects;

/**
 * 优化内容 包装类,返回视图层所需的字段
 *
 * @author BladeX
 * @since 2024-08-04
 */
public class OptimizeWrapper extends BaseEntityWrapper<OptimizeEntity, OptimizeVO>  {

	public static OptimizeWrapper build() {
		return new OptimizeWrapper();
 	}

	@Override
	public OptimizeVO entityVO(OptimizeEntity optimize) {
		OptimizeVO optimizeVO = Objects.requireNonNull(BeanUtil.copy(optimize, OptimizeVO.class));

		//User createUser = UserCache.getUser(optimize.getCreateUser());
		//User updateUser = UserCache.getUser(optimize.getUpdateUser());
		//optimizeVO.setCreateUserName(createUser.getName());
		//optimizeVO.setUpdateUserName(updateUser.getName());

		return optimizeVO;
	}


}
