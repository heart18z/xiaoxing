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
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 实体类
 *
 * @author Chill
 */
@Data
@TableName("blade_code")
@Schema(name = "Code对象", description = "Code对象")
public class Code implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "主键")
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	private Long id;

	/**
	 * 数据模型主键
	 */
	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "数据模型主键")
	private Long modelId;

	/**
	 * 上级菜单主键
	 */
	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "上级菜单主键")
	private Long menuId;

	/**
	 * 模块名称
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
	 * 实体名
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
	 * 模版类型
	 */
	@Schema(description = "模版类型")
	private String templateType;

	/**
	 * 作者信息
	 */
	@Schema(description = "作者信息")
	private String author;

	/**
	 * 子表模型主键
	 */
	@Schema(description = "子表模型主键")
	private String subModelId;

	/**
	 * 子表绑定外键
	 */
	@Schema(description = "子表绑定外键")
	private String subFkId;

	/**
	 * 树主键字段
	 */
	@Schema(description = "树主键字段")
	private String treeId;

	/**
	 * 树父主键字段
	 */
	@Schema(description = "树父主键字段")
	private String treePid;

	/**
	 * 树名称字段
	 */
	@Schema(description = "树名称字段")
	private String treeName;

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

	/**
	 * 是否已删除
	 */
	@TableLogic
	@Schema(description = "是否已删除")
	private Integer isDeleted;


}
