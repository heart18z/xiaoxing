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
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.EqualsAndHashCode;
import org.springblade.core.tenant.mp.TenantEntity;

/**
 * 统一消息发送表 实体类
 *
 * @author BladeX
 * @since 2023-12-11
 */
@Data
@TableName("blade_unified_messaging_send")
@Schema(name = "UnifiedMessagingSend对象", description = "统一消息发送表")
@EqualsAndHashCode(callSuper = true)
public class UnifiedMessagingSendEntity extends TenantEntity {

	/**
	 * 通知标题
	 */
	@Schema(description = "通知标题")
	private String noticeTitle;
	/**
	 * 通知内容
	 */
	@Schema(description = "通知内容")
	private String noticeContent;
	/**
	 * 通知状态
	 */
	@Schema(description = "通知状态")
	private Integer noticeStatus;
	/**
	 * 发送频率
	 */
	@Schema(description = "发送频率")
	private String sendRate;
	/**
	 * 发送类型
	 */
	@Schema(description = "发送类型")
	private String sendType;
	/**
	 * 首次发送时间
	 */
	@Schema(description = "首次发送时间")
	private Date sentTime;
	/**
	 * 截至发送时间
	 */
	@Schema(description = "截至发送时间")
	private Date endSentTime;
	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;
	/**
	 * 链接
	 */
	@Schema(description = "链接")
	private String href;
	/**
	 * 关联数据id
	 */
	@Schema(description = "关联数据id")
	private String fromId;
	/**
	 * 发送目标账号
	 */
	@Schema(description = "发送目标账号")
	private String targetAccount;


	/**
	 * 发送目标账号
	 */
	@Schema(description = "发送目标姓名")
	private String targetName;


	private String messageSystemId;

	private Integer messageSystemHttpCode;

}
