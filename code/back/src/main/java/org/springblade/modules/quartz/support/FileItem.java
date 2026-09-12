package org.springblade.modules.quartz.support;

import lombok.Data;

import java.util.Date;

@Data
public class FileItem {
	/**
	 * 文件原表主键
	 */
	private Long sourceId;
	/**
	 * 原系统文件编号（唯一）
	 */
	private String fileSourceNum;
	/**
	 * 文件名称
	 */
	private String fileName;
	/**
	 * 文件路径
	 */
	private String url;
	/**
	 * 物业地址编码
	 */
	private String propertyCode;
	/**
	 * 原系统上传人
	 */
	private String uploadUserName;
	/**
	 * 原系统文件上传时间
	 */
	private Date uploadTime;

}
