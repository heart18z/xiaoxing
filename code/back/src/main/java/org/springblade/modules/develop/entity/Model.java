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
package org.springblade.modules.develop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.BaseEntity;

/**
 * 数据模型表实体类
 *
 * @author Chill
 */
@Data
@TableName("blade_model")
@EqualsAndHashCode(callSuper = true)
@Schema(name = "Model对象", description = "数据模型表")
public class Model extends BaseEntity {

	private static final long serialVersionUID = 1L;

	/**
	 * 数据源主键
	 */
	@Schema(description = "数据源主键")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long datasourceId;
	/**
	 * 模型名称
	 */
	@Schema(description = "模型名称")
	private String modelName;
	/**
	 * 模型编号
	 */
	@Schema(description = "模型编号")
	private String modelCode;
	/**
	 * 物理表名
	 */
	@Schema(description = "物理表名")
	private String modelTable;
	/**
	 * 模型类名
	 */
	@Schema(description = "模型类名")
	private String modelClass;
	/**
	 * 模型备注
	 */
	@Schema(description = "模型备注")
	private String modelRemark;


}
