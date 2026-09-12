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
package org.springblade.modules.aiconfig.excel;


import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serializable;


/**
 * AI按钮接口配置表 Excel实体类
 *
 * @author wxd
 * @since 2026-03-25
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class BtnConfigExcel implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 按钮名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("按钮名称")
	private String btnName;
	/**
	 * 按钮编号
	 */
	@ColumnWidth(20)
	@ExcelProperty("按钮编号")
	private String btnNo;
	/**
	 * AI接口地址
	 */
	@ColumnWidth(20)
	@ExcelProperty("AI接口地址")
	private String aiInterfaceUrl;
	/**
	 * 提示词
	 */
	@ColumnWidth(20)
	@ExcelProperty("提示词")
	private String prompt;
	/**
	 * 密钥
	 */
	@ColumnWidth(20)
	@ExcelProperty("密钥")
	private String secretKey;
	/**
	 * 是否已删除
	 */
	@ColumnWidth(20)
	@ExcelProperty("是否已删除")
	private Integer isDeleted;

}
