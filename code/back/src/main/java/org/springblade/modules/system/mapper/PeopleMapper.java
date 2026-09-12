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
package org.springblade.modules.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springblade.modules.system.entity.PeopleEntity;
import org.springblade.modules.system.vo.PeopleVO;
import org.springblade.modules.system.excel.PeopleExcel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.Map;

/**
 * 人员 Mapper 接口
 *
 * @author BladeX
 * @since 2024-01-15
 */
public interface PeopleMapper extends BaseMapper<PeopleEntity> {

	/**
	 * 自定义分页
	 *
	 * @param page
	 * @param people
	 * @return
	 */
	List<PeopleVO> selectPeoplePage(IPage page, PeopleVO people);


	/**
	 * 获取导出数据
	 *
	 * @param queryWrapper
	 * @return
	 */
	List<PeopleExcel> exportPeople(@Param("ew") Wrapper<PeopleEntity> queryWrapper);

	List<Long> selectUserIdsByPeopleGroupIds(@Param("peopleGroupIds") List<Long> peopleGroupIds);
}
