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
package org.springblade.modules.desk.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.desk.entity.GroupUserEntity;
import org.springblade.modules.desk.excel.GroupUserExcel;
import org.springblade.modules.desk.vo.GroupUserVO;

import java.util.List;

/**
 * 用户分组表 服务类
 *
 * @author BladeX
 * @since 2023-08-07
 */
public interface IGroupUserService extends IService<GroupUserEntity> {
	/**
	 * 自定义分页
	 *
	 * @param page
	 * @param groupUser
	 * @return
	 */
	IPage<GroupUserVO> selectGroupUserPage(IPage<GroupUserVO> page, GroupUserVO groupUser);


	/**
	 * 导出数据
	 *
	 * @param queryWrapper
	 * @return
	 */
	List<GroupUserExcel> exportGroupUser(Wrapper<GroupUserEntity> queryWrapper);

}
