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
import org.springblade.modules.system.entity.RemoteParamEntity;
import org.springblade.modules.system.vo.RemoteParamVO;
import java.util.Objects;

/**
 * ai专用应用字典
 包装类,返回视图层所需的字段
 *
 * @author BladeX
 * @since 2025-03-18
 */
public class RemoteParamWrapper extends BaseEntityWrapper<RemoteParamEntity, RemoteParamVO>  {

	public static RemoteParamWrapper build() {
		return new RemoteParamWrapper();
 	}

	@Override
	public RemoteParamVO entityVO(RemoteParamEntity remoteParam) {
		RemoteParamVO remoteParamVO = Objects.requireNonNull(BeanUtil.copy(remoteParam, RemoteParamVO.class));

		//User createUser = UserCache.getUser(remoteParam.getCreateUser());
		//User updateUser = UserCache.getUser(remoteParam.getUpdateUser());
		//remoteParamVO.setCreateUserName(createUser.getName());
		//remoteParamVO.setUpdateUserName(updateUser.getName());

		return remoteParamVO;
	}


}
