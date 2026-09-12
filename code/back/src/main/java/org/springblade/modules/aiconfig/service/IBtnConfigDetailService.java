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
package org.springblade.modules.aiconfig.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import org.springblade.modules.aiconfig.entity.BtnConfigDetailEntity;
import org.springblade.modules.aiconfig.excel.BtnConfigDetailExcel;
import org.springblade.modules.aiconfig.vo.BtnConfigDetailVO;

import java.util.List;

/**
 * AI按钮接口配置详情表 服务类
 *
 * @author wxd
 * @since 2026-03-30
 */
public interface IBtnConfigDetailService extends BaseService<BtnConfigDetailEntity> {
	/**
	 * 自定义分页
	 *
	 * @param page
	 * @param btnConfigDetail
	 * @return
	 */
	IPage<BtnConfigDetailVO> selectBtnConfigDetailPage(IPage<BtnConfigDetailVO> page, BtnConfigDetailVO btnConfigDetail);


	/**
	 * 导出数据
	 *
	 * @param queryWrapper
	 * @return
	 */
	List<BtnConfigDetailExcel> exportBtnConfigDetail(Wrapper<BtnConfigDetailEntity> queryWrapper);

}
