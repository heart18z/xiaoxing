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
package org.springblade.modules.resource.excel;


import lombok.Data;

import java.util.Date;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import java.io.Serializable;


/**
 * 文件权限修改日志 Excel实体类
 *
 * @author BladeX
 * @since 2024-08-20
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class AttachPermissionLogExcel implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 创建用户
	 */
	@ColumnWidth(20)
	@ExcelProperty("创建用户")
	private Long createBy;
	/**
	 * 授权用户账号
	 */
	@ColumnWidth(20)
	@ExcelProperty("授权用户账号")
	private String permissonAccount;
	/**
	 * 附件id
	 */
	@ColumnWidth(20)
	@ExcelProperty("附件id")
	private Long attachSourceId;

}
