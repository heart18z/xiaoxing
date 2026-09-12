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
package org.springblade.modules.aiconfig.wrapper;

import org.springblade.common.cache.UserCache;
import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.aiconfig.entity.BtnConfigEntity;
import org.springblade.modules.aiconfig.vo.BtnConfigVO;
import org.springblade.modules.system.entity.User;

import java.util.Objects;

/**
 * AI按钮接口配置表 包装类,返回视图层所需的字段
 *
 * @author wxd
 * @since 2026-03-25
 */
public class BtnConfigWrapper extends BaseEntityWrapper<BtnConfigEntity, BtnConfigVO>  {

	public static BtnConfigWrapper build() {
		return new BtnConfigWrapper();
 	}

	@Override
	public BtnConfigVO entityVO(BtnConfigEntity btnConfig) {
		BtnConfigVO btnConfigVO = Objects.requireNonNull(BeanUtil.copy(btnConfig, BtnConfigVO.class));

		User createUser = UserCache.getUser(btnConfig.getCreateUser());
		if (Func.isNotEmpty(createUser)){
			btnConfigVO.setCreateUserName(createUser.getRealName());
		}
		User updateUser = UserCache.getUser(btnConfig.getUpdateUser());
		if (Func.isNotEmpty(updateUser)){
			btnConfigVO.setUpdateUserName(updateUser.getRealName());
		}

		return btnConfigVO;
	}


}
