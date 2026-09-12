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
package org.springblade.modules.develop.wrapper;

import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.modules.develop.entity.ThirdPartyTableEntity;
import org.springblade.modules.develop.vo.ThirdPartyTableVO;
import java.util.Objects;

/**
 * 中台数据 包装类,返回视图层所需的字段
 *
 * @author BladeX
 * @since 2025-02-09
 */
public class ThirdPartyTableWrapper extends BaseEntityWrapper<ThirdPartyTableEntity, ThirdPartyTableVO>  {

	public static ThirdPartyTableWrapper build() {
		return new ThirdPartyTableWrapper();
 	}

	@Override
	public ThirdPartyTableVO entityVO(ThirdPartyTableEntity thirdPartyTable) {
		ThirdPartyTableVO thirdPartyTableVO = Objects.requireNonNull(BeanUtil.copy(thirdPartyTable, ThirdPartyTableVO.class));

		//User createUser = UserCache.getUser(thirdPartyTable.getCreateUser());
		//User updateUser = UserCache.getUser(thirdPartyTable.getUpdateUser());
		//thirdPartyTableVO.setCreateUserName(createUser.getName());
		//thirdPartyTableVO.setUpdateUserName(updateUser.getName());

		return thirdPartyTableVO;
	}


}
