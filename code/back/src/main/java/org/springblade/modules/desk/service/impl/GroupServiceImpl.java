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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.tool.node.ForestNodeMerger;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.desk.entity.GroupEntity;
import org.springblade.modules.desk.entity.GroupUserEntity;
import org.springblade.modules.desk.excel.GroupExcel;
import org.springblade.modules.desk.mapper.GroupMapper;
import org.springblade.modules.desk.service.IGroupService;
import org.springblade.modules.desk.service.IGroupUserService;
import org.springblade.modules.desk.vo.GroupUserVO;
import org.springblade.modules.desk.vo.GroupVO;
import org.springblade.modules.desk.wrapper.GroupWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户分组 服务实现类
 *
 * @author BladeX
 * @since 2023-08-07
 */
@Service
@AllArgsConstructor
public class GroupServiceImpl extends BaseServiceImpl<GroupMapper, GroupEntity> implements IGroupService {

    private final static String PARENT_ID = "parentId";

    private final static String PARENT_ID_EQUAL = PARENT_ID+"_equal";
	private final IGroupUserService groupUserService;


	@Override
	public IPage<GroupVO> selectGroupPage(IPage<GroupVO> page, GroupVO group) {
		return page.setRecords(baseMapper.selectGroupPage(page, group));
	}

	@Override
	public List<GroupVO> tree() {
		return ForestNodeMerger.merge(baseMapper.tree());
	}

    @Override
    public IPage<GroupVO> lazyList(IPage<GroupVO> page, Map<String, Object> param) {
         if (Func.isEmpty(Func.toStr(param.get(PARENT_ID)))) {
            List<Object> valueList = param.values().stream().filter(v->
                Func.isNotEmpty(Func.toStr(v))
            ).collect(Collectors.toList());
            if (valueList.size() == 2) {
                param.put(PARENT_ID_EQUAL,0);
            } else {
                param.put(PARENT_ID_EQUAL,null);
            }
        } else {
            Object parentId = param.get(PARENT_ID);
            param.clear();
            param.put(PARENT_ID_EQUAL,parentId);
        }
        QueryWrapper<GroupEntity> queryWrapper = Condition.getQueryWrapper(param, GroupEntity.class);
        List<GroupVO> list = this.baseMapper.lazyList(queryWrapper,page);
        return page.setRecords(GroupWrapper.build().listNodeLazyVO(list));
    }

	@Override
	public List<GroupExcel> exportGroup(Wrapper<GroupEntity> queryWrapper) {
		List<GroupExcel> groupList = baseMapper.exportGroup(queryWrapper);
		//groupList.forEach(group -> {
		//	group.setTypeName(DictCache.getValue(DictEnum.YES_NO, Group.getType()));
		//});
		return groupList;
	}

	@Override
	public List<GroupUserVO> getGroupUserList(Long groupId) {
		return this.baseMapper.selectGroupUserList(groupId);
	}

	@Override
	@Transactional
	public Boolean groupUserSubmit(GroupVO group) {
		if (group.getId() == null) {
			throw new ServiceException("分组不得为空!");
		}

		List<Long> userIds = group.getUserId();
		if (userIds!= null) {
			this.groupUserService.remove(new LambdaQueryWrapper<GroupUserEntity>()
				.eq(GroupUserEntity::getGroupId,group.getId()));
			List<GroupUserEntity> userEntities = new ArrayList<>();
			for(Long userid: userIds) {
				GroupUserEntity user = new GroupUserEntity();
				user.setGroupId(group.getId());
				user.setUserId(userid);
				userEntities.add(user);
			}
			groupUserService.saveBatch(userEntities);
		}
		return true;
	}

}
