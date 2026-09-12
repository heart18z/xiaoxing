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
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.BaseEntity;


/**
 * 业务参数表实体类
 *
 * @author BladeX
 * @since 2022-03-14
 */
@Data
@TableName("blade_biz_param")
@EqualsAndHashCode(callSuper = true)
@Schema(name = "BizParam对象", description = "业务参数表")
public class BizParam extends BaseEntity {

	private static final long serialVersionUID = 1L;

	/**
	 * 父机构ID
	 */
	@Schema(description = "父机构ID")
	private Long parentId;
	/**
	 * 参数名
	 */
	@Schema(description = "参数名")
	private String paramName;
	/**
	 * 参数键
	 */
	@Schema(description = "参数键")
	private String paramKey;
	/**
	 * 参数值
	 */
	@Schema(description = "参数值")
	private String paramValue;
	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;
	/**
	 * 参数层级限制
	 */
	@Schema(description = "参数层级限制")
	private Integer hierarchical;
	/**
	 * 排序
	 */
	@Schema(description = "排序")
	@JsonSerialize(
		using = ToStringSerializer.class
	)
	private Integer sort;
	/**
	 * 当前层级
	 */
	@Schema(description = "当前层级")
	private Integer currentHierarchy;
	/**
	 * 是否是同步数据
	 */
	private String isSync;
	/**
	 * 初始来源
	 */
	private String systemSource;
	/**
	 * 公共参数
	 */
	private String isPublicParam;

}
