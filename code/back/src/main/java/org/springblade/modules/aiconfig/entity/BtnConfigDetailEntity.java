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
package org.springblade.modules.aiconfig.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.BaseEntity;

/**
 * AI按钮接口配置详情表 实体类
 *
 * @author wxd
 * @since 2026-03-30
 */
@Data
@TableName("blade_ai_btn_config_detail")
@Schema(name = "BtnConfigDetail对象", description = "AI按钮接口配置详情表")
@EqualsAndHashCode(callSuper = true)
public class BtnConfigDetailEntity extends BaseEntity {

	/**
	 * 按钮配置ID
	 */
	@Schema(description = "按钮配置ID")
	private Long btnConfigId;
	/**
	 * 数据分类
	 */
	@Schema(description = "数据分类")
	private String dataType;
	/**
	 * 数据类名称
	 */
	@Schema(description = "数据类名称")
	private String dataName;
	/**
	 * 数据类说明
	 */
	@Schema(description = "数据类说明")
	private String dataDesc;
	/**
	 * 数据类结构
	 */
	@Schema(description = "数据类结构")
	private String dataStructure;
	/**
	 * 具体描述
	 */
	@Schema(description = "具体描述")
	private String specificDescription;

	private String tableName;

	private String fieldName;

	private String filterConditions;

	private String mockData;

}
