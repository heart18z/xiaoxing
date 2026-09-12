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
package org.springblade.modules.standard.excel;


import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * 业务库表 Excel实体类
 *
 * @author BladeX
 * @since 2023-07-21
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class BizMenuTableExcel implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 表名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("表名称")
	private String tableName;
	/**
	 * 关联菜单
	 */
	@ColumnWidth(20)
	@ExcelProperty("关联菜单")
	private Long menuId;
	/**
	 * 表作用描述
	 */
	@ColumnWidth(20)
	@ExcelProperty("表作用描述")
	private String tableFunc;
	/**
	 * 表创建时间
	 */
	@ColumnWidth(20)
	@ExcelProperty("表创建时间")
	private Date tableCreateTime;
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
