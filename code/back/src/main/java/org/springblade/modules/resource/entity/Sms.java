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
package org.springblade.modules.resource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.tenant.mp.TenantEntity;

/**
 * 短信配置表实体类
 *
 * @author BladeX
 */
@Data
@TableName("blade_sms")
@EqualsAndHashCode(callSuper = true)
@Schema(name = "Sms对象", description = "短信配置表")
public class Sms extends TenantEntity {

	private static final long serialVersionUID = 1L;

	/**
	 * 资源编号
	 */
	@Schema(description = "资源编号")
	private String smsCode;

	/**
	 * 模板ID
	 */
	@Schema(description = "模板ID")
	private String templateId;
	/**
	 * 分类
	 */
	@Schema(description = "分类")
	private Integer category;
	/**
	 * accessKey
	 */
	@Schema(description = "accessKey")
	private String accessKey;
	/**
	 * secretKey
	 */
	@Schema(description = "secretKey")
	private String secretKey;
	/**
	 * regionId
	 */
	@Schema(description = "regionId")
	private String regionId;
	/**
	 * 短信签名
	 */
	@Schema(description = "短信签名")
	private String signName;
	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;


}
