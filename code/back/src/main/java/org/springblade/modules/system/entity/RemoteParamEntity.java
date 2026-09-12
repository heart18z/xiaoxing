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
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.BaseEntity;

/**
 * ai专用应用字典
 实体类
 *
 * @author BladeX
 * @since 2025-03-18
 */
@Data
@TableName("blade_remote_param")
@Schema(name = "RemoteParam对象", description = "ai专用应用字典 ")
@EqualsAndHashCode(callSuper = true)
public class RemoteParamEntity extends BaseEntity {

	/**
	 * 创建租户
	 */
	@Schema(description = "创建租户")
	private String createTenant;
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
	 * 排序
	 */
	@Schema(description = "排序")
	private Integer sort;
	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;
	/**
	 * 层级限制
	 */
	@Schema(description = "层级限制")
	private Integer hierarchical;
	/**
	 * 当前层级
	 */
	@Schema(description = "当前层级")
	private Integer currentHierarchy;
	/**
	 * 是否是同步数据
	 */
	@Schema(description = "是否是同步数据")
	private String isSync;
	/**
	 * 初始来源
	 */
	@Schema(description = "初始来源")
	private String systemSource;
	/**
	 * 公共参数
	 */
	@Schema(description = "公共参数")
	private String isPublicParam;

}
