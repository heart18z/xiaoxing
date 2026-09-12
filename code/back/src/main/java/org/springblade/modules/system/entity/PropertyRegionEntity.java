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
 * 物业信息表 实体类
 *
 * @author BladeX
 * @since 2024-08-14
 */
@Data
@TableName("blade_property_region")
@Schema(name = "PropertyRegion对象", description = "物业信息表")
@EqualsAndHashCode(callSuper = true)
public class PropertyRegionEntity extends TenantEntity {

	/**
	 * 物业号
	 */
	@Schema(description = "物业号")
	private String propertyCode;
	/**
	 *
	 */
	@Schema(description = "")
	private String propertyName;
	/**
	 * 管理人
	 */
	@Schema(description = "管理人")
	private String admin;
	/**
	 * 是否是同步过来的数据
	 */
	@Schema(description = "是否是同步过来的数据")
	private String isSync;

}
