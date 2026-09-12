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
import org.springblade.modules.aiconfig.entity.BtnConfigEntity;
import org.springblade.modules.aiconfig.excel.BtnConfigExcel;
import org.springblade.modules.aiconfig.vo.BtnConfigVO;

import java.util.List;

/**
 * AI按钮接口配置表 服务类
 *
 * @author wxd
 * @since 2026-03-25
 */
public interface IBtnConfigService extends BaseService<BtnConfigEntity> {
	/**
	 * 详情
	 * @param btnConfig
	 * @return
	 */
	BtnConfigEntity detail(BtnConfigEntity btnConfig);

	/**
	 * 自定义分页
	 *
	 * @param page
	 * @param btnConfig
	 * @return
	 */
	IPage<BtnConfigVO> selectBtnConfigPage(IPage<BtnConfigVO> page, BtnConfigVO btnConfig);

	/**
	 * 提交
	 * @param btnConfig
	 * @return
	 */
	boolean submit(BtnConfigEntity btnConfig);


	/**
	 * 导出数据
	 *
	 * @param queryWrapper
	 * @return
	 */
	List<BtnConfigExcel> exportBtnConfig(Wrapper<BtnConfigEntity> queryWrapper);

}
