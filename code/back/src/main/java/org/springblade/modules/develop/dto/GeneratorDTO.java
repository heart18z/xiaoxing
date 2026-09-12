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
package org.springblade.modules.develop.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 代码生成DTO
 *
 * @author Chill
 */
@Data
@Schema(description = "代码生成DTO")
public class GeneratorDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 上级菜单主键
	 */
	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "上级菜单主键")
	private Long menuId;

	/**
	 * 数据源主键
	 */
	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "数据源主键")
	private Long datasourceId;

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
	 * 表单设计
	 */
	@Schema(description = "表单设计")
	private String modelForm;

	/**
	 * 模型类名
	 */
	@Schema(description = "模型类名")
	private String modelClass;

	/**
	 * 服务名称
	 */
	@Schema(description = "服务名称")
	private String serviceName;

	/**
	 * 模块名称
	 */
	@Schema(description = "模块名称")
	private String codeName;

	/**
	 * 表名
	 */
	@Schema(description = "表名")
	private String tableName;

	/**
	 * 表前缀
	 */
	@Schema(description = "表前缀")
	private String tablePrefix;

	/**
	 * 主键名
	 */
	@Schema(description = "主键名")
	private String pkName;

	/**
	 * 后端包名
	 */
	@Schema(description = "后端包名")
	private String packageName;

	/**
	 * 基础业务模式
	 */
	@Schema(description = "基础业务模式")
	private Integer baseMode;

	/**
	 * 包装器模式
	 */
	@Schema(description = "包装器模式")
	private Integer wrapMode;

	/**
	 * 远程调用模式
	 */
	@Schema(description = "远程调用模式")
	private Integer feignMode;

	/**
	 * 代码风格
	 */
	@Schema(description = "代码风格")
	private String codeStyle;

	/**
	 * 后端路径
	 */
	@Schema(description = "后端路径")
	private String apiPath;

	/**
	 * 前端路径
	 */
	@Schema(description = "前端路径")
	private String webPath;

}
