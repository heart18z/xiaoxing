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
import org.springblade.core.mp.base.BaseService;
import org.springblade.modules.desk.entity.GroupEntity;
import org.springblade.modules.desk.excel.GroupExcel;
import org.springblade.modules.desk.vo.GroupUserVO;
import org.springblade.modules.desk.vo.GroupVO;

import java.util.List;
import java.util.Map;

/**
 * 用户分组 服务类
 *
 * @author BladeX
 * @since 2023-08-07
 */
public interface IGroupService extends BaseService<GroupEntity> {
	/**
	 * 自定义分页
	 *
	 * @param page
	 * @param group
	 * @return
	 */
	IPage<GroupVO> selectGroupPage(IPage<GroupVO> page, GroupVO group);

	/**
	 * 树形结构
	 *
	 * @param
	 * @return
	 */
	List<GroupVO> tree();

    /**
     * 树表懒加载
     * @param page
     * @param param
     * @return
     */
    IPage<GroupVO> lazyList(IPage<GroupVO> page, Map<String, Object> param);

	/**
	 * 导出数据
	 *
	 * @param queryWrapper
	 * @return
	 */
	List<GroupExcel> exportGroup(Wrapper<GroupEntity> queryWrapper);

	List<GroupUserVO> getGroupUserList(Long groupId);

	Boolean groupUserSubmit(GroupVO group);
}
