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
package org.springblade.modules.develop.excel;


import lombok.Data;

import java.util.Date;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import java.io.Serializable;


/**
 * 数据中台列信息 Excel实体类
 *
 * @author BladeX
 * @since 2025-02-09
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class ThirdPartyTableColumnExcel implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 租户ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("租户ID")
	private String tenantId;
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
	 * 第三方表id
	 */
	@ColumnWidth(20)
	@ExcelProperty("第三方表id")
	private Long tableId;
	/**
	 * 字段名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("字段名称")
	private String columnName;
	/**
	 * 列类型
	 */
	@ColumnWidth(20)
	@ExcelProperty("列类型")
	private String columnType;
	/**
	 * 长度
	 */
	@ColumnWidth(20)
	@ExcelProperty("长度")
	private Integer columnLong;
	/**
	 * 小数点
	 */
	@ColumnWidth(20)
	@ExcelProperty("小数点")
	private Integer columnDecimal;
	/**
	 * 是否必填
	 */
	@ColumnWidth(20)
	@ExcelProperty("是否必填")
	private String columnNotNull;
	/**
	 * 是否主键
	 */
	@ColumnWidth(20)
	@ExcelProperty("是否主键")
	private String columnIsPrimaryKey;
	/**
	 * 字段注释
	 */
	@ColumnWidth(20)
	@ExcelProperty("字段注释")
	private String columnComment;

}
