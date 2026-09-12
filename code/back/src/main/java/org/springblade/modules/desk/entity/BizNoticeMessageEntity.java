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
 * 公告通知消息表 实体类
 *
 * @author BladeX
 * @since 2023-07-31
 */
@Data
@TableName("blade_biz_notice_message")
@Schema(name = "BizNoticeMessage对象", description = "公告通知消息表")
@EqualsAndHashCode(callSuper = true)
public class BizNoticeMessageEntity extends TenantEntity {

	/**
	 * 通知id
	 */
	@Schema(description = "通知id")
	private Long noticeId;
	/**
	 * 消息回复
	 */
	@Schema(description = "消息回复")
	private String noticeReply;
	/**
	 * 消息查阅状态
	 */
	@Schema(description = "消息查阅状态")
	private String noticeReadStatus;


	private Date noticeReplyTime;
	/**
	 * 消息查阅时间
	 */
	@Schema(description = "消息查阅时间")
	private Date noticeReadTime;
	/**
	 * 被通知人id
	 */
	@Schema(description = "被通知人id")
	private Long receiveUserId;
	/**
	 * 发送人id
	 */
	@Schema(description = "发送人id")
	private Long sendUserId;
	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;

}
