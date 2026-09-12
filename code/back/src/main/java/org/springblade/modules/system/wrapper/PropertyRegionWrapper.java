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
import org.springblade.modules.system.entity.PropertyRegionEntity;
import org.springblade.modules.system.vo.PropertyRegionVO;
import java.util.Objects;

/**
 * 物业信息表 包装类,返回视图层所需的字段
 *
 * @author BladeX
 * @since 2024-08-14
 */
public class PropertyRegionWrapper extends BaseEntityWrapper<PropertyRegionEntity, PropertyRegionVO>  {

	public static PropertyRegionWrapper build() {
		return new PropertyRegionWrapper();
 	}

	@Override
	public PropertyRegionVO entityVO(PropertyRegionEntity propertyRegion) {
		PropertyRegionVO propertyRegionVO = Objects.requireNonNull(BeanUtil.copy(propertyRegion, PropertyRegionVO.class));

		//User createUser = UserCache.getUser(propertyRegion.getCreateUser());
		//User updateUser = UserCache.getUser(propertyRegion.getUpdateUser());
		//propertyRegionVO.setCreateUserName(createUser.getName());
		//propertyRegionVO.setUpdateUserName(updateUser.getName());

		return propertyRegionVO;
	}


}
