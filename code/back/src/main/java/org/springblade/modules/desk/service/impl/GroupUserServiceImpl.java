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
package org.springblade.modules.desk.service.impl;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.modules.desk.entity.GroupUserEntity;
import org.springblade.modules.desk.excel.GroupUserExcel;
import org.springblade.modules.desk.mapper.GroupUserMapper;
import org.springblade.modules.desk.service.IGroupUserService;
import org.springblade.modules.desk.vo.GroupUserVO;
import org.springframework.stereotype.Service;

import java.util.List;
/**
 * 用户分组表 服务实现类
 *
 * @author BladeX
 * @since 2023-08-07
 */
@Service
public class GroupUserServiceImpl extends ServiceImpl<GroupUserMapper, GroupUserEntity> implements IGroupUserService {


	@Override
	public IPage<GroupUserVO> selectGroupUserPage(IPage<GroupUserVO> page, GroupUserVO groupUser) {
		return page.setRecords(baseMapper.selectGroupUserPage(page, groupUser));
	}


	@Override
	public List<GroupUserExcel> exportGroupUser(Wrapper<GroupUserEntity> queryWrapper) {
		List<GroupUserExcel> groupUserList = baseMapper.exportGroupUser(queryWrapper);
		//groupUserList.forEach(groupUser -> {
		//	groupUser.setTypeName(DictCache.getValue(DictEnum.YES_NO, GroupUser.getType()));
		//});
		return groupUserList;
	}

}
