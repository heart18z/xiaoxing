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
package org.springblade.modules.develop.service;

import org.springblade.modules.develop.dto.GeneratorDTO;

import java.util.List;

/**
 * 代码生成 服务类
 *
 * @author Chill
 */
public interface IGenerateService {

	/**
	 * 生成代码
	 *
	 * @param ids 主键集合
	 * @return boolean
	 */
	boolean code(List<Long> ids);

	/**
	 * 快速生成代码
	 *
	 * @param dto 配置参数
	 * @return boolean
	 */
	boolean codeFast(GeneratorDTO dto);

}
