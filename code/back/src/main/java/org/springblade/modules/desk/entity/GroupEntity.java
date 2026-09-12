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
package org.springblade.modules.desk.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.tenant.mp.TenantEntity;

/**
 * 用户分组 实体类
 *
 * @author BladeX
 * @since 2023-08-07
 */
@Data
@TableName("blade_group")
@Schema(name = "Group对象", description = "用户分组")
@EqualsAndHashCode(callSuper = true)
public class GroupEntity extends TenantEntity {

	/**
	 * 父主键
	 */
	@Schema(description = "父主键")
	private Long parentId;
	/**
	 * 祖级列表
	 */
	@Schema(description = "祖级列表")
	private String ancestors;
	/**
	 * 分组名
	 */
	@Schema(description = "分组名")
	private String groupName;
	/**
	 * 分组全称
	 */
	@Schema(description = "分组全称")
	private String groupFullName;
	/**
	 * 排序
	 */
	@Schema(description = "排序")
	private Integer sort;
	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;

}
