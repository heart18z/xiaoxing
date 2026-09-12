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
import org.springblade.modules.system.entity.PeopleEntity;
import org.springblade.modules.system.vo.PeopleVO;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 人员 包装类,返回视图层所需的字段
 *
 * @author BladeX
 * @since 2024-01-15
 */
public class PeopleWrapper extends BaseEntityWrapper<PeopleEntity, PeopleVO>  {

	public static PeopleWrapper build() {
		return new PeopleWrapper();
 	}

	@Override
	public PeopleVO entityVO(PeopleEntity people) {
		PeopleVO peopleVO = Objects.requireNonNull(BeanUtil.copy(people, PeopleVO.class));

		//User createUser = UserCache.getUser(people.getCreateUser());
		//User updateUser = UserCache.getUser(people.getUpdateUser());
		//peopleVO.setCreateUserName(createUser.getName());
		//peopleVO.setUpdateUserName(updateUser.getName());

		return peopleVO;
	}

	public List<PeopleVO> selectVO(List<PeopleEntity> list) {


		return (List)list.stream().map(this::entityVO).map(
			m->{
				m.setLabel(m.getRealName()+"("+m.getAccount()+")");
				return m;
			}
		).collect(Collectors.toList());
	}


}
