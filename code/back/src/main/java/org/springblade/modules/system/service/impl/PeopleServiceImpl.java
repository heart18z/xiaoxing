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
package org.springblade.modules.system.service.impl;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springblade.modules.system.entity.PeopleEntity;
import org.springblade.modules.system.entity.PeopleGroup;
import org.springblade.modules.system.entity.PeopleGroupChild;
import org.springblade.modules.system.mapper.PeopleGroupMapper;

import org.springblade.modules.system.service.IPeopleGroupChildService;
import org.springblade.modules.system.vo.PeopleVO;
import org.springblade.modules.system.excel.PeopleExcel;
import org.springblade.modules.system.mapper.PeopleMapper;
import org.springblade.modules.system.service.IPeopleService;
import org.springblade.modules.system.wrapper.PeopleWrapper;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 人员 服务实现类
 *
 * @author BladeX
 * @since 2024-01-15
 */
@Service
public class PeopleServiceImpl extends BaseServiceImpl<PeopleMapper, PeopleEntity> implements IPeopleService {
	@Resource
	IPeopleGroupChildService peopleGroupChildService;
	@Resource
	private PeopleGroupMapper peopleGroupMapper;

	@Override
	public IPage<PeopleVO> selectPeoplePage(IPage<PeopleVO> page, PeopleVO people) {
		return page.setRecords(baseMapper.selectPeoplePage(page, people));
	}


	@Override
	public List<PeopleExcel> exportPeople(Wrapper<PeopleEntity> queryWrapper) {
		List<PeopleExcel> peopleList = baseMapper.exportPeople(queryWrapper);
		//peopleList.forEach(people -> {
		//	people.setTypeName(DictCache.getValue(DictEnum.YES_NO, People.getType()));
		//});
		return peopleList;
	}

	@Override
	public IPage<PeopleEntity> getListPage(IPage<PeopleEntity> page, PeopleVO peopleQo) {
		LambdaQueryWrapper<PeopleEntity> lqw = Wrappers.lambdaQuery();
		if (StrUtil.isNotBlank(peopleQo.getKeyword())) {
			lqw.like(StrUtil.isNotBlank(peopleQo.getKeyword()), PeopleEntity::getPhone, peopleQo.getKeyword())
				.or().like(StrUtil.isNotBlank(peopleQo.getKeyword()), PeopleEntity::getRealName, peopleQo.getKeyword())
				.orderByAsc(PeopleEntity::getRealName);
		} else {
			if (CollectionUtil.isNotEmpty(peopleQo.getPeopleGroupIds())) {
				List<Long> userIds = baseMapper.selectUserIdsByPeopleGroupIds(peopleQo.getPeopleGroupIds());
				if (CollectionUtil.isNotEmpty(userIds)) {
					lqw.in(PeopleEntity::getId, userIds);
				}
			}
			lqw.like(StrUtil.isNotBlank(peopleQo.getPhone()), PeopleEntity::getPhone, peopleQo.getPhone())
				.like(StrUtil.isNotBlank(peopleQo.getRealName()), PeopleEntity::getRealName, peopleQo.getRealName())
				.like(StrUtil.isNotBlank(peopleQo.getRemark()), PeopleEntity::getRemark, peopleQo.getRemark())
				.in(CollectionUtil.isNotEmpty(peopleQo.getIds()), PeopleEntity::getId, peopleQo.getIds())
				.notIn(CollectionUtil.isNotEmpty(peopleQo.getExcludeIds()), PeopleEntity::getId, peopleQo.getExcludeIds())
				.orderByAsc(PeopleEntity::getRealName);
		}
		IPage<PeopleEntity> iPage = baseMapper.selectPage(page, lqw);
		iPage.getRecords().forEach(item -> {
			if (StrUtil.isNotBlank(item.getPhone())) {
				item.setPhone(DesensitizedUtil.mobilePhone(item.getPhone()));
			}
			List<Long> groupIds = peopleGroupMapper.selectGroupIdByUserId(item.getId());
			if (CollectionUtil.isNotEmpty(groupIds)) {
				List<PeopleGroup> peopleGroupList = peopleGroupMapper.selectList(new LambdaQueryWrapper<PeopleGroup>().in(PeopleGroup::getId, groupIds));
				item.setPeopleGroupNames(CollectionUtil.join(peopleGroupList.stream().map(item2 -> item2.getGroupName()).collect(Collectors.toList()), ","));
			}else {
				item.setPeopleGroupNames("");
			}
//			if(StrUtil.isNotBlank(item.getAccount())){
//				item.setAccount(DesensitizedUtil.mobilePhone(item.getAccount()));
//			}
		});
		return iPage;
	}

	@Override
	public List<PeopleEntity> getPeopleListByIds(PeopleEntity people) {
		LambdaQueryWrapper<PeopleEntity> lqw = Wrappers.lambdaQuery();
		if (CollectionUtil.isNotEmpty(people.getIds())) {
			lqw.in(PeopleEntity::getId, people.getIds());
			return baseMapper.selectList(lqw);
		} else {
			return new ArrayList<>();
		}
	}

	@Override
	public PeopleVO detail(Long id) {
		PeopleEntity people = baseMapper.selectById(id);
		PeopleVO peopleVO = PeopleWrapper.build().entityVO(people);
		List<Long> groupIds = peopleGroupChildService.list(new LambdaQueryWrapper<PeopleGroupChild>()
			.eq(PeopleGroupChild::getPeopleId, id)).stream().map(PeopleGroupChild::getPeopleGroupId).collect(Collectors.toList());
		peopleVO.setPeopleGroupIds(groupIds);
		return peopleVO;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submit(PeopleVO people) {
		List<Long> groupIds = people.getPeopleGroupIds();
		if (groupIds!=null && !groupIds.isEmpty() && people.getId() != null) {
			peopleGroupChildService.remove(new LambdaQueryWrapper<PeopleGroupChild>().eq(PeopleGroupChild::getPeopleId, people.getId()));
			List<PeopleGroupChild> peopleGroupChildList = groupIds.stream().map(item -> {
				PeopleGroupChild peopleGroupChild = new PeopleGroupChild();
				peopleGroupChild.setPeopleId(people.getId());
				peopleGroupChild.setPeopleGroupId(item);
				return peopleGroupChild;
			}).collect(Collectors.toList());
			peopleGroupChildService.saveBatch(peopleGroupChildList);
		}
		return this.saveOrUpdate(people);
	}


}
