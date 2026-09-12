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
package org.springblade.modules.desk.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.tenant.mp.TenantEntity;

import java.util.Date;

/**
 * 定时任务数据同步日志表 实体类
 *
 * @author BladeX
 * @since 2024-08-05
 */
@Data
@TableName("blade_scheduled_log")
@Schema(name = "ScheduledLog对象", description = "定时任务日志表")
@EqualsAndHashCode(callSuper = true)
public class ScheduledLogEntity extends TenantEntity {

	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;
	/**
	 * 任务编码
	 */
	@Schema(description = "任务编码")
	private String taskCode;
	/**
	 * 任务名称
	 */
	@Schema(description = "任务名称")
	private String taskName;
	/**
	 * 任务内容
	 */
	@Schema(description = "任务内容")
	private String taskContent;
	/**
	 * 执行时间
	 */
	@Schema(description = "执行时间")
	private Date execTime;
	/**
	 * 结束时间
	 */
	@Schema(description = "结束时间")
	private Date endTime;
	/**
	 * 是否成功
	 */
	@Schema(description = "是否成功")
	private Integer success;
	/**
	 * 执行条数
	 */
	@Schema(description = "执行条数")
	private Integer updateRows;

	/**
	 * 触发方式
	 */
	private String triggerMode;

}
