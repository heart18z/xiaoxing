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

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.EqualsAndHashCode;
import org.springblade.core.tenant.mp.TenantEntity;

/**
 * 注释 实体类
 *
 * @author BladeX
 * @since 2024-01-17
 */
@Data
@TableName("blade_note")
@Schema(name = "Note对象", description = "注释")
@EqualsAndHashCode(callSuper = true)
public class NoteEntity extends TenantEntity {

	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;
	/**
	 * 注释名称
	 */
	@Schema(description = "注释名称")
	private String name;
	/**
	 * 注释类型
	 */
	@Schema(description = "注释类型")
	private String type;
	/**
	 * 注释参数
	 */
	@Schema(description = "注释参数")
	private String params;
	/**
	 * 注释内容
	 */
	@Schema(description = "注释内容")
	private String content;
	/**
	 * 类型图标
	 */
	@Schema(description = "类型图标")
	private String icon;

}
