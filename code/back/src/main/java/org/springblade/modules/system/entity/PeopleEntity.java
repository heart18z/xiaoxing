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
package org.springblade.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import java.util.List;

import lombok.EqualsAndHashCode;
import org.springblade.core.tenant.mp.TenantEntity;

/**
 * 人员 实体类
 *
 * @author BladeX
 * @since 2024-01-15
 */
@Data
@TableName("blade_people")
@Schema(name = "People对象", description = "人员")
@EqualsAndHashCode(callSuper = true)
public class PeopleEntity extends TenantEntity {

	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;
	/**
	 * 账号
	 */
	@Schema(description = "账号")
	private String account;
	/**
	 * 真名
	 */
	@Schema(description = "真名")
	private String realName;
	/**
	 * 邮箱
	 */
	@Schema(description = "邮箱")
	private String email;
	/**
	 * 手机
	 */
	@Schema(description = "手机")
	private String phone;
	/**
	 * 微信
	 */
	@Schema(description = "微信")
	private String wechat;
	/**
	 * 性别
	 */
	@Schema(description = "性别")
	private Integer sex;
	/**
	 * 人司关系
	 */
	@Schema(description = "人司关系")
	private String relation;

	/**
	 * 是否是同步过来的数据
	 */
	@Schema(description = "是否是同步过来的数据")
	private String isSync;

	private Date birthday;

	private String avatar;

	private String otherDescribe;

	@TableField(exist = false)
	private String peopleGroupNames;

	@TableField(exist = false)
	private List<Long> ids;

	private String postId;

	private String deptId;

	private String idNo;

}
