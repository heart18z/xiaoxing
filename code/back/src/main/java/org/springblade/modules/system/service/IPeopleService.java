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
package org.springblade.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.springblade.modules.system.entity.PeopleEntity;

import org.springblade.modules.system.vo.PeopleVO;
import org.springblade.modules.system.excel.PeopleExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.support.Query;
import org.springblade.core.mp.base.BaseService;
import java.util.List;
import java.util.Map;

/**
 * 人员 服务类
 *
 * @author BladeX
 * @since 2024-01-15
 */
public interface IPeopleService extends BaseService<PeopleEntity> {
	/**
	 * 自定义分页
	 *
	 * @param page
	 * @param people
	 * @return
	 */
	IPage<PeopleVO> selectPeoplePage(IPage<PeopleVO> page, PeopleVO people);


	/**
	 * 导出数据
	 *
	 * @param queryWrapper
	 * @return
	 */
	List<PeopleExcel> exportPeople(Wrapper<PeopleEntity> queryWrapper);

	/**
	 * 获取分页列表
	 *
	 * @param page
	 * @param peopleQo
	 * @return
	 */
	IPage<PeopleEntity> getListPage(IPage<PeopleEntity> page, PeopleVO peopleQo);

	List<PeopleEntity> getPeopleListByIds(PeopleEntity people);

    PeopleVO detail(Long id);

	boolean submit(PeopleVO people);
}
