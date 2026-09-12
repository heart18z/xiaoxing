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
 * 注释设置 Excel实体类
 *
 * @author BladeX
 * @since 2024-01-17
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class NoteSettingExcel implements Serializable {

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
	 * 菜单id
	 */
	@ColumnWidth(20)
	@ExcelProperty("菜单id")
	private Long menuId;
	/**
	 * 字段属性
	 */
	@ColumnWidth(20)
	@ExcelProperty("字段属性")
	private String propCode;
	/**
	 * 字段名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("字段名称")
	private String propName;
	/**
	 * 字段页面（展示位置）
	 */
	@ColumnWidth(20)
	@ExcelProperty("字段页面（展示位置）")
	private String propLocation;
	/**
	 * 字段显示设置（展示条件）
	 */
	@ColumnWidth(20)
	@ExcelProperty("字段显示设置（展示条件）")
	private String displaySet;
	/**
	 * 注释表id
	 */
	@ColumnWidth(20)
	@ExcelProperty("注释表id")
	private Long noteId;

}
