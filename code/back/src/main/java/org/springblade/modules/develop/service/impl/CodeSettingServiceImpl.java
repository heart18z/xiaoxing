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
package org.springblade.modules.develop.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.modules.develop.entity.CodeSetting;
import org.springblade.modules.develop.mapper.CodeSettingMapper;
import org.springblade.modules.develop.service.ICodeSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 代码生成器配置表 服务实现类
 *
 * @author Chill
 */
@Service
public class CodeSettingServiceImpl extends ServiceImpl<CodeSettingMapper, CodeSetting> implements ICodeSettingService {

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean enable(Long id) {
		boolean temp1 = this.update(Wrappers.<CodeSetting>update().lambda().set(CodeSetting::getStatus, BladeConstant.DB_STATUS_1));
		boolean temp2 = this.update(Wrappers.<CodeSetting>update().lambda().set(CodeSetting::getStatus, BladeConstant.DB_STATUS_2).eq(CodeSetting::getId, id));
		return temp1 && temp2;
	}

}
