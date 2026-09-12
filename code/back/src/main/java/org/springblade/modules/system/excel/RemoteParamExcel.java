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

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import java.io.Serializable;


/**
 * ai专用应用字典
 Excel实体类
 *
 * @author BladeX
 * @since 2025-03-18
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class RemoteParamExcel implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 创建租户
	 */
	@ColumnWidth(20)
	@ExcelProperty("创建租户")
	private String createTenant;
	/**
	 * 父机构ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("父机构ID")
	private Long parentId;
	/**
	 * 参数名
	 */
	@ColumnWidth(20)
	@ExcelProperty("参数名")
	private String paramName;
	/**
	 * 参数键
	 */
	@ColumnWidth(20)
	@ExcelProperty("参数键")
	private String paramKey;
	/**
	 * 参数值
	 */
	@ColumnWidth(20)
	@ExcelProperty("参数值")
	private String paramValue;
	/**
	 * 排序
	 */
	@ColumnWidth(20)
	@ExcelProperty("排序")
	private Integer sort;
	/**
	 * 备注
	 */
	@ColumnWidth(20)
	@ExcelProperty("备注")
	private String remark;
	/**
	 * 层级限制
	 */
	@ColumnWidth(20)
	@ExcelProperty("层级限制")
	private Integer hierarchical;
	/**
	 * 当前层级
	 */
	@ColumnWidth(20)
	@ExcelProperty("当前层级")
	private Integer currentHierarchy;
	/**
	 * 是否已删除
	 */
	@ColumnWidth(20)
	@ExcelProperty("是否已删除")
	private Integer isDeleted;
	/**
	 * 是否是同步数据
	 */
	@ColumnWidth(20)
	@ExcelProperty("是否是同步数据")
	private String isSync;
	/**
	 * 初始来源
	 */
	@ColumnWidth(20)
	@ExcelProperty("初始来源")
	private String systemSource;
	/**
	 * 公共参数
	 */
	@ColumnWidth(20)
	@ExcelProperty("公共参数")
	private String isPublicParam;

}
