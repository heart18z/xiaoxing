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
 * 注释设置 实体类
 *
 * @author BladeX
 * @since 2024-01-17
 */
@Data
@TableName("blade_note_setting")
@Schema(name = "NoteSetting对象", description = "注释设置")
@EqualsAndHashCode(callSuper = true)
public class NoteSettingEntity extends TenantEntity {

	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;
	/**
	 * 菜单id
	 */
	@Schema(description = "菜单id")
	private Long menuId;
	/**
	 * 字段属性
	 */
	@Schema(description = "字段属性")
	private String propCode;
	/**
	 * 字段名称
	 */
	@Schema(description = "字段名称")
	private String propName;
	/**
	 * 字段页面（展示位置）
	 */
	@Schema(description = "字段页面（展示位置）")
	private String propLocation;
	/**
	 * 字段显示设置（展示条件）
	 */
	@Schema(description = "字段显示设置（展示条件）")
	private String displaySet;
	/**
	 * 注释表id
	 */
	@Schema(description = "注释表id")
	private Long noteId;

	private String content;

}
