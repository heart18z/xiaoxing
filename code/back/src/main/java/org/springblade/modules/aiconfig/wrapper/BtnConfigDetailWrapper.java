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

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.modules.aiconfig.entity.BtnConfigDetailEntity;
import org.springblade.modules.aiconfig.vo.BtnConfigDetailVO;

import java.util.Objects;

/**
 * AI按钮接口配置详情表 包装类,返回视图层所需的字段
 *
 * @author wxd
 * @since 2026-03-30
 */
public class BtnConfigDetailWrapper extends BaseEntityWrapper<BtnConfigDetailEntity, BtnConfigDetailVO>  {

	public static BtnConfigDetailWrapper build() {
		return new BtnConfigDetailWrapper();
 	}

	@Override
	public BtnConfigDetailVO entityVO(BtnConfigDetailEntity btnConfigDetail) {
		BtnConfigDetailVO btnConfigDetailVO = Objects.requireNonNull(BeanUtil.copy(btnConfigDetail, BtnConfigDetailVO.class));

		//User createUser = UserCache.getUser(btnConfigDetail.getCreateUser());
		//User updateUser = UserCache.getUser(btnConfigDetail.getUpdateUser());
		//btnConfigDetailVO.setCreateUserName(createUser.getName());
		//btnConfigDetailVO.setUpdateUserName(updateUser.getName());

		return btnConfigDetailVO;
	}


}
