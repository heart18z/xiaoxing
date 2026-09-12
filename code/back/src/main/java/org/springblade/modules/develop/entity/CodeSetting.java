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

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 代码生成器配置表 实体类
 *
 * @author Chill
 */
@Data
@TableName("blade_code_setting")
@Schema(name = "CodeSetting对象", description = "CodeSetting对象")
public class CodeSetting implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "主键")
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	private Long id;

	/**
	 * 名称
	 */
	@Schema(description = "名称")
	private String name;

	/**
	 * 编号
	 */
	@Schema(description = "编号")
	private String code;

	/**
	 * 分类[1:默认配置 2:表单设计]
	 */
	@Schema(description = "分类", hidden = true)
	private Integer category;

	/**
	 * 配置项
	 */
	@Schema(description = "配置项")
	private String settings;

	/**
	 * 状态[1:正常 2:启用]
	 */
	@Schema(description = "状态", hidden = true)
	private Integer status;

	/**
	 * 是否已删除
	 */
	@Schema(description = "是否已删除")
	private Integer isDeleted;

}
