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
package org.springblade.modules.standard.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.tenant.mp.TenantEntity;

import java.util.Date;

/**
 * 业务库表 实体类
 *
 * @author BladeX
 * @since 2023-07-21
 */
@Data
@TableName("blade_biz_menu_table")
@Schema(name = "BizMenuTable对象", description = "业务库表")
@EqualsAndHashCode(callSuper = true)
public class BizMenuTableEntity extends TenantEntity {

	/**
	 * 表名称
	 */
	@Schema(description = "表名称")
	private String tableName;
	/**
	 * 关联菜单
	 */
	@Schema(description = "关联菜单")
	private Long menuId;
	/**
	 * 表作用描述
	 */
	@Schema(description = "表作用描述")
	private String tableFunc;
	/**
	 * 表创建时间
	 */
	@Schema(description = "表创建时间")
	private Date tableCreateTime;
	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;

	private Long datasourceId;

	private String tableType;

}
