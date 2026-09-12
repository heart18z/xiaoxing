package org.springblade.modules.resource.vo;

import lombok.Data;

import java.util.List;

@Data
public class FileUpLoadVO {
	/**
	 * 文件id
	 */
	private Long attachId;
	/**
	 * 文件来源id
	 */
	private Long sourceId;
	/**
	 * 菜单路径
	 */
	private String path;
	/**
	 * 上传类别
	 */
	private Integer type;
	/**
	 * 文件类别
	 */
	private String attachExtension;
	/**
	 * 文件自定义名称
	 */
	private String diyFileName;

	private Long originalId;



}
