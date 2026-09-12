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
package org.springblade.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.core.tool.utils.Func;

import java.io.Serializable;
import java.util.Objects;

/**
 * 实体类
 *
 * @author Chill
 */
@Data
@TableName("blade_menu")
@Schema(name = "Menu对象", description = "Menu对象")
public class Menu implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "主键")
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	private Long id;

	/**
	 * 菜单父主键
	 */
	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "菜单父主键")
	private Long parentId;

	/**
	 * 菜单编号
	 */
	@Schema(description = "菜单编号")
	private String code;

	/**
	 * 菜单名称
	 */
	@Schema(description = "菜单名称")
	private String name;

	/**
	 * 菜单别名
	 */
	@Schema(description = "菜单别名")
	private String alias;

	/**
	 * 请求地址
	 */
	@Schema(description = "请求地址")
	private String path;

	/**
	 * 菜单资源
	 */
	@Schema(description = "菜单资源")
	private String source;

	/**
	 * 排序
	 */
	@Schema(description = "排序")
	private Integer sort;

	/**
	 * 菜单类型
	 */
	@Schema(description = "菜单类型")
	private Integer category;

	/**
	 * 操作按钮类型
	 */
	@Schema(description = "操作按钮类型")
	private Integer action;

	/**
	 * 是否打开新页面
	 */
	@Schema(description = "是否打开新页面")
	private Integer isOpen;

	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;

	/**
	 * 是否已删除
	 */
	@TableLogic
	@Schema(description = "是否已删除")
	private Integer isDeleted;


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		Menu other = (Menu) obj;
		if (Func.equals(this.getId(), other.getId())) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, parentId, code);
	}


}
