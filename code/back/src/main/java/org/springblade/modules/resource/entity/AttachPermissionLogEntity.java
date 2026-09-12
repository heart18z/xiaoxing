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
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.io.Serializable;

/**
 * 文件权限修改日志 实体类
 *
 * @author BladeX
 * @since 2024-08-20
 */
@Data
@TableName("blade_attach_permission_log")
@Schema(name = "AttachPermissionLog对象", description = "文件权限修改日志")
public class AttachPermissionLogEntity implements Serializable {

	private static final long serialVersionUID = 1L;
	/**
	 * 主键
	 */
	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "主键")
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	private Long id;

	/**
	 * 创建用户
	 */
	@Schema(description = "创建用户")
	private Long createBy;

	@JsonFormat(
		pattern = "yyyy-MM-dd HH:mm:ss"
	)
	private Date createTime;
	/**
	 * 授权用户账号
	 */
	@Schema(description = "授权用户账号")
	private String permissionAccount;
	/**
	 * 附件id
	 */
	@Schema(description = "附件id")
	private Long attachSourceId;

}
