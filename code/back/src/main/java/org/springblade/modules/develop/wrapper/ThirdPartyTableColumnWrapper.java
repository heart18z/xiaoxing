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
import org.springblade.modules.develop.entity.ThirdPartyTableColumnEntity;
import org.springblade.modules.develop.vo.ThirdPartyTableColumnVO;

import java.util.Objects;

/**
 * 数据中台列信息 包装类,返回视图层所需的字段
 *
 * @author BladeX
 * @since 2025-02-09
 */
public class ThirdPartyTableColumnWrapper extends BaseEntityWrapper<ThirdPartyTableColumnEntity, ThirdPartyTableColumnVO>  {

	public static ThirdPartyTableColumnWrapper build() {
		return new ThirdPartyTableColumnWrapper();
 	}

	@Override
	public ThirdPartyTableColumnVO entityVO(ThirdPartyTableColumnEntity thirdPartyTableColumn) {
		ThirdPartyTableColumnVO thirdPartyTableColumnVO = Objects.requireNonNull(BeanUtil.copy(thirdPartyTableColumn, ThirdPartyTableColumnVO.class));

		//User createUser = UserCache.getUser(thirdPartyTableColumn.getCreateUser());
		//User updateUser = UserCache.getUser(thirdPartyTableColumn.getUpdateUser());
		//thirdPartyTableColumnVO.setCreateUserName(createUser.getName());
		//thirdPartyTableColumnVO.setUpdateUserName(updateUser.getName());

		return thirdPartyTableColumnVO;
	}


}
