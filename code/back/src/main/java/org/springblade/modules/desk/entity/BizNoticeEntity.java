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
 * 公告通知表 实体类
 *
 * @author BladeX
 * @since 2023-07-31
 */
@Data
@TableName("blade_biz_notice")
@Schema(name = "BizNotice对象", description = "公告通知表")
@EqualsAndHashCode(callSuper = true)
public class BizNoticeEntity extends TenantEntity {

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
	 * 通知发送时间
	 */
	@Schema(description = "通知发送时间")
	private Date noticeSentTime;
	/**
	 * 通知创建时间
	 */
	@Schema(description = "通知创建时间")
	private Date noticeBuildTime;
	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;
	/**
	 * 类型
	 */
	@Schema(description = "类型")
	private String category;
	/**
	 * 是否显示
	 */
	@Schema(description = "是否显示")
	private Integer isShow;
	/**
	 * 链接
	 */
	@Schema(description = "链接")
	private String href;
	/**
	 * 描述
	 */
	@Schema(description = "描述")
	private String description;
	/**
	 * 发送类型
	 */
	@Schema(description = "发送类型")
	private Integer sendType;

}
