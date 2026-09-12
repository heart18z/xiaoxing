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
 * 附件来源表 实体类
 *
 * @author BladeX
 * @since 2023-06-10
 */
@Data
@TableName("blade_attach_source")
@Schema(name = "AttachSource对象", description = "附件来源表")
@EqualsAndHashCode(callSuper = true)
public class AttachSourceEntity extends TenantEntity {

	/**
	 * 来源id
	 */
	@Schema(description = "来源id")
	private Long sourceId;
	/**
	 * 文件表id
	 */
	@Schema(description = "文件表id")
	private Long attachId;
	/**
	 * 来源类别 1-页面 2-数据列
	 */
	@Schema(description = "来源类别 1-页面 2-数据列")
	private Integer sourceType;
	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;
	/**
	 * 文件类别
	 */
	@Schema(description = "文件类别")
	private String attachExtension;

	/**
	 * 文件自定义名称
	 */
	@Schema(description = "自定义名称")
	private String diyFileName;

	/**
	 * 文件的物理存放位置
	 */
	@Schema(description = "文件的物理存放位置")
	private String filePhysicalPosition;
	/**
	 * 是否文件实物保存
	 */
	private String isPhysicalSave;
	/**
	 * 文件编号
	 */
	private String fileNum;
	/**
	 * 是否保密
	 */
	private String isSecrecy;
	/**
	 * 保管状态
	 */
	private String safekeepStatus;
	/**
	 * 存档编号
	 */
	private String archiveNo;

	/**
	 * 保密用户
	 */
	private String 	permissionUserAccount;
	/**
	 * 上传类型（1、本地上传；2、文件库引用、3、链接上传）
	 */
	private String uploadType;
	/**
	 * 文件地址
	 */
	private String link;
	/**
	 * 文件详细存放地址
	 */
	private String physicalPositionInfo;
	/**
	 * 实物保管人
	 */
	private String keeper;
	/**
	 * 排序
	 */
	private Integer sort;

	private String signStatus;

	private Long originalId;
}
