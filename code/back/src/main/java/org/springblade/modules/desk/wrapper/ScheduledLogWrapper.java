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
import org.springblade.modules.desk.entity.ScheduledLogEntity;
import org.springblade.modules.desk.vo.ScheduledLogVO;

import java.util.Objects;

/**
 * 定时任务日志表 包装类,返回视图层所需的字段
 *
 * @author BladeX
 * @since 2024-08-05
 */
public class ScheduledLogWrapper extends BaseEntityWrapper<ScheduledLogEntity, ScheduledLogVO>  {

	public static ScheduledLogWrapper build() {
		return new ScheduledLogWrapper();
 	}

	@Override
	public ScheduledLogVO entityVO(ScheduledLogEntity scheduledLog) {
		ScheduledLogVO scheduledLogVO = Objects.requireNonNull(BeanUtil.copy(scheduledLog, ScheduledLogVO.class));

		//User createUser = UserCache.getUser(scheduledLog.getCreateUser());
		//User updateUser = UserCache.getUser(scheduledLog.getUpdateUser());
		//scheduledLogVO.setCreateUserName(createUser.getName());
		//scheduledLogVO.setUpdateUserName(updateUser.getName());

		return scheduledLogVO;
	}


}
