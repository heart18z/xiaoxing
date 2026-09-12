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
package org.springblade.modules.desk.excel;


import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * 公告通知消息表 Excel实体类
 *
 * @author BladeX
 * @since 2023-07-31
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class BizNoticeMessageExcel implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 通知id
	 */
	@ColumnWidth(20)
	@ExcelProperty("通知id")
	private Long noticeId;
	/**
	 * 消息回复
	 */
	@ColumnWidth(20)
	@ExcelProperty("消息回复")
	private String noticeReply;
	/**
	 * 消息查阅状态
	 */
	@ColumnWidth(20)
	@ExcelProperty("消息查阅状态")
	private String noticeReadStatus;
	/**
	 * 消息查阅时间
	 */
	@ColumnWidth(20)
	@ExcelProperty("消息查阅时间")
	private Date noticeReadTime;
	/**
	 * 被通知人id
	 */
	@ColumnWidth(20)
	@ExcelProperty("被通知人id")
	private Long receiveUserId;
	/**
	 * 发送人id
	 */
	@ColumnWidth(20)
	@ExcelProperty("发送人id")
	private Long sendUserId;
	/**
	 * 备注
	 */
	@ColumnWidth(20)
	@ExcelProperty("备注")
	private String remark;
	/**
	 * 是否已删除
	 */
	@ColumnWidth(20)
	@ExcelProperty("是否已删除")
	private Integer isDeleted;
	/**
	 * 租户ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("租户ID")
	private String tenantId;

}
