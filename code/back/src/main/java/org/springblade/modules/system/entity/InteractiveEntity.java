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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.NullSerializer;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.EqualsAndHashCode;
import org.springblade.core.tenant.mp.TenantEntity;

/**
 * 对外交互 实体类
 *
 * @author BladeX
 * @since 2024-08-20
 */
@Data
@TableName("blade_interactive")
@Schema(name = "Interactive对象", description = "对外交互")
@EqualsAndHashCode(callSuper = true)
public class InteractiveEntity extends TenantEntity {

	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;
	/**
	 * 交互名称
	 */
	@Schema(description = "交互名称")
	private String interactiveName;
	/**
	 * 交互类型
	 */
	@Schema(description = "交互类型")
	private String interactiveType;
	/**
	 * 交互方式
	 */
	@Schema(description = "交互方式")
	private String interactiveMethod;
	/**
	 * 对方名称
	 */
	@Schema(description = "对方名称")
	private String targetSystem;
	/**
	 * 主要用途
	 */
	@Schema(description = "主要用途")
	private String purpose;
	/**
	 * 对接地址
	 */
	@Schema(description = "对接地址")
	private String address;

	private String taskCode;

	private String interactiveCreate;

	private String interactiveCode;

	private String syncRule;

	private String relatedTableName;
	@JsonSerialize(nullsUsing = NullSerializer.class)
	private Long datasourceId;

	private String syncRuleRemark;

}
