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
package org.springblade.modules.standard.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import org.springblade.modules.standard.entity.StandardEntity;
import org.springblade.modules.standard.excel.StandardExcel;
import org.springblade.modules.standard.vo.StandardNodeVO;
import org.springblade.modules.standard.vo.StandardVO;

import java.util.List;
import java.util.Map;

/**
 * 规范类列表 服务类
 *
 * @author linchaofan
 * @since 2023-06-09
 */
public interface IStandardService extends BaseService<StandardEntity> {
	/**
	 * 自定义分页
	 *
	 * @param page
	 * @param standard
	 * @return
	 */
	IPage<StandardVO> selectStandardPage(IPage<StandardVO> page, StandardVO standard);


	/**
	 * 导出数据
	 *
	 * @param queryWrapper
	 * @return
	 */
	List<StandardExcel> exportStandard(Wrapper<StandardEntity> queryWrapper);

	/**
	 * 懒加载列表
	 *
	 * @param parentId
	 * @param param
	 * @return
	 */
	List<StandardVO> lazyList(Long parentId, Map<String, Object> param);

	/**
	 * 懒加载总数
	 *
	 * @param parentId
	 * @param param
	 * @return
	 */
	Integer lazyListCount(Long parentId, Map<String, Object> param);


	Boolean submit(StandardEntity standard);

	List<StandardNodeVO> tree();
}
