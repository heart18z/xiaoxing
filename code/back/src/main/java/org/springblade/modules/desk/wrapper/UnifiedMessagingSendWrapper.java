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
package org.springblade.modules.desk.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.modules.desk.entity.UnifiedMessagingSendEntity;
import org.springblade.modules.desk.vo.UnifiedMessagingSendVO;
import java.util.Objects;

/**
 * 统一消息发送表 包装类,返回视图层所需的字段
 *
 * @author BladeX
 * @since 2023-12-11
 */
public class UnifiedMessagingSendWrapper extends BaseEntityWrapper<UnifiedMessagingSendEntity, UnifiedMessagingSendVO>  {

	public static UnifiedMessagingSendWrapper build() {
		return new UnifiedMessagingSendWrapper();
 	}

	@Override
	public UnifiedMessagingSendVO entityVO(UnifiedMessagingSendEntity unifiedMessagingSend) {
		UnifiedMessagingSendVO unifiedMessagingSendVO = Objects.requireNonNull(BeanUtil.copy(unifiedMessagingSend, UnifiedMessagingSendVO.class));

		//User createUser = UserCache.getUser(unifiedMessagingSend.getCreateUser());
		//User updateUser = UserCache.getUser(unifiedMessagingSend.getUpdateUser());
		//unifiedMessagingSendVO.setCreateUserName(createUser.getName());
		//unifiedMessagingSendVO.setUpdateUserName(updateUser.getName());

		return unifiedMessagingSendVO;
	}


}
