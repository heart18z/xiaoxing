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
import org.springblade.modules.desk.entity.BizNoticeMessageEntity;
import org.springblade.modules.desk.vo.BizNoticeMessageVO;

import java.util.Objects;

/**
 * 公告通知消息表 包装类,返回视图层所需的字段
 *
 * @author BladeX
 * @since 2023-07-31
 */
public class BizNoticeMessageWrapper extends BaseEntityWrapper<BizNoticeMessageEntity, BizNoticeMessageVO>  {

	public static BizNoticeMessageWrapper build() {
		return new BizNoticeMessageWrapper();
 	}

	@Override
	public BizNoticeMessageVO entityVO(BizNoticeMessageEntity bizNoticeMessage) {
		BizNoticeMessageVO bizNoticeMessageVO = Objects.requireNonNull(BeanUtil.copy(bizNoticeMessage, BizNoticeMessageVO.class));

		//User createUser = UserCache.getUser(bizNoticeMessage.getCreateUser());
		//User updateUser = UserCache.getUser(bizNoticeMessage.getUpdateUser());
		//bizNoticeMessageVO.setCreateUserName(createUser.getName());
		//bizNoticeMessageVO.setUpdateUserName(updateUser.getName());

		return bizNoticeMessageVO;
	}


}
