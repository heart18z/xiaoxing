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
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.EqualsAndHashCode;
import org.springblade.core.tenant.mp.TenantEntity;

/**
 * 中台数据 实体类
 *
 * @author BladeX
 * @since 2025-02-09
 */
@Data
@TableName("blade_third_party_table")
@Schema(name = "ThirdPartyTable对象", description = "中台数据")
@EqualsAndHashCode(callSuper = true)
public class ThirdPartyTableEntity extends TenantEntity {

	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;
	/**
	 * 表名称
	 */
	@Schema(description = "表名称")
	private String tableName;
	/**
	 * 表注释
	 */
	@Schema(description = "表注释")
	private String tableComment;
	/**
	 * 数据库链接
	 */
	@Schema(description = "数据库链接")
	private Long databaseId;
	/**
	 * 用途
	 */
	@Schema(description = "用途")
	private String purpose;

	private String sourceSystem;

	private String sourceSystemDeveloper;

}
