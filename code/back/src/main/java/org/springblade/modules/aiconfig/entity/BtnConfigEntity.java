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
package org.springblade.modules.aiconfig.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.BaseEntity;

import java.util.List;

/**
 * AI按钮接口配置表 实体类
 *
 * @author wxd
 * @since 2026-03-25
 */
@Data
@TableName("blade_ai_btn_config")
@Schema(name = "BtnConfig对象", description = "AI按钮接口配置表")
@EqualsAndHashCode(callSuper = true)
public class BtnConfigEntity extends BaseEntity {

	/**
	 * 按钮名称
	 */
	@Schema(description = "按钮名称")
	private String btnName;
	/**
	 * 按钮编号
	 */
	@Schema(description = "按钮编号")
	private String btnNo;
	/**
	 * 页面路径
	 */
	@Schema(description = "页面路径")
	private String pagePaths;
	/**
	 * 按钮说明
	 */
	@Schema(description = "按钮说明")
	private String btnDesc;
	/**
	 * 按钮类型
	 */
	@Schema(description = "按钮类型")
	private String btnType;
	/**
	 * 字段输出格式要求
	 */
	@Schema(description = "字段输出格式要求")
	private String outputFormatRequirement;
	/**
	 * 输出格式说明
	 */
	@Schema(description = "输出格式说明")
	private String outputFormatDesc;
	/**
	 * 中台交互类型
	 */
	@Schema(description = "中台交互类型")
	private String interactionType;
	/**
	 * AI接口地址
	 */
	@Schema(description = "AI接口地址")
	private String aiInterfaceUrl;
	/**
	 * 提示词
	 */
	@Schema(description = "提示词")
	private String prompt;
	/**
	 * 引导回复（对话输入框占位文案）
	 */
	@Schema(description = "引导回复")
	private String guideReply;
	/**
	 * 密钥
	 */
	@Schema(description = "密钥")
	private String secretKey;

	/**
	 * 创建人
	 */
	@TableField(exist = false)
	@Schema(description = "创建人")
	private String createUserName;

	/**
	 * 更新人
	 */
	@TableField(exist = false)
	@Schema(description = "更新人")
	private String updateUserName;

	/**
	 * 详情列表
	 */
	@TableField(exist = false)
	@Schema(description = "详情列表")
	private List<BtnConfigDetailEntity> detailList;

}
