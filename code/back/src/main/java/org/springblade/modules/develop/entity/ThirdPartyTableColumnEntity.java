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
import com.fasterxml.jackson.databind.ser.std.NullSerializer;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.EqualsAndHashCode;
import org.springblade.core.tenant.mp.TenantEntity;

/**
 * 数据中台列信息 实体类
 *
 * @author BladeX
 * @since 2025-02-09
 */
@Data
@TableName("blade_third_party_table_column")
@Schema(name = "ThirdPartyTableColumn对象", description = "数据中台列信息")
@EqualsAndHashCode(callSuper = true)
public class ThirdPartyTableColumnEntity extends TenantEntity {

	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;
	/**
	 * 第三方表id
	 */
	@Schema(description = "第三方表id")
	private Long tableId;
	/**
	 * 字段名称
	 */
	@Schema(description = "字段名称")
	private String columnName;
	/**
	 * 列类型
	 */
	@Schema(description = "列类型")
	private String columnType;
	/**
	 * 长度
	 */
	@Schema(description = "长度")
	@JsonSerialize(nullsUsing = NullSerializer.class)
	private Integer columnLong;
	/**
	 * 小数点
	 */
	@Schema(description = "小数点")
	@JsonSerialize(nullsUsing = NullSerializer.class)
	private Integer columnDecimal;
	/**
	 * 是否必填
	 */
	@Schema(description = "是否必填")
	private String columnNotNull;
	/**
	 * 是否主键
	 */
	@Schema(description = "是否主键")
	private String columnIsPrimaryKey;
	/**
	 * 字段注释
	 */
	@Schema(description = "字段注释")
	private String columnComment;

	private Integer columnSort;

	private String columnInfoDesc;

	private String columnInfoName;

	private String columnInfoDataStandard;

}
