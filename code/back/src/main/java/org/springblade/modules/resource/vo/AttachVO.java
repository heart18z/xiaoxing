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
package org.springblade.modules.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.modules.resource.entity.Attach;

import java.util.List;

/**
 * 附件表视图实体类
 *
 * @author Chill
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "AttachVO对象", description = "附件表")
public class AttachVO extends Attach {
	private static final long serialVersionUID = 1L;

	private Long attachSourceId;

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

	private Long sourceId;

	private Integer sourceType;

	private String ossBucketName;

	private Boolean temFile;

	List<Long> attachIds;

	List<Long> sourceIds;

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

	private boolean permission;

	/**
	 * 上传类型（1、本地上传；2、文件库引用、3、链接上传）
	 */
	private String uploadType;

	/**
	 * 文件详细存放地址
	 */
	private String physicalPositionInfo;
	/**
	 * 实物保管人
	 */
	private String keeper;

	private String createUserAccount;

	private Integer isNotProperty;

	private Integer sort;

	private String signStatus;
}
