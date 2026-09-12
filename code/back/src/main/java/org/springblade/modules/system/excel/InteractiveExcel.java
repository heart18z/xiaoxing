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
package org.springblade.modules.system.excel;


import lombok.Data;

import java.util.Date;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import java.io.Serializable;


/**
 * 对外交互 Excel实体类
 *
 * @author BladeX
 * @since 2024-08-20
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class InteractiveExcel implements Serializable {

	private static final long serialVersionUID = 1L;

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
	/**
	 * 交互名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("交互名称")
	private String interactiveName;
	/**
	 * 交互类型
	 */
	@ColumnWidth(20)
	@ExcelProperty("交互类型")
	private String interactiveType;
	/**
	 * 交互方式
	 */
	@ColumnWidth(20)
	@ExcelProperty("交互方式")
	private String interactiveMethod;
	/**
	 * 对方名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("对方名称")
	private String targetSystem;
	/**
	 * 主要用途
	 */
	@ColumnWidth(20)
	@ExcelProperty("主要用途")
	private String purpose;
	/**
	 * 对接地址
	 */
	@ColumnWidth(20)
	@ExcelProperty("对接地址")
	private String address;

}
