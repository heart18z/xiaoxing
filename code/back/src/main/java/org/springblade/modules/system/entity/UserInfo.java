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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.core.tool.support.Kv;

import java.io.Serializable;
import java.util.List;

/**
 * 用户信息
 *
 * @author Chill
 */
@Data
@Schema(description = "用户信息")
public class UserInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 用户基础信息
	 */
	@Schema(description = "用户")
	private User user;

	/**
	 * 拓展信息
	 */
	@Schema(description = "拓展信息")
	private Kv detail;

	/**
	 * 权限标识集合
	 */
	@Schema(description = "权限集合")
	private List<String> permissions;

	/**
	 * 角色集合
	 */
	@Schema(description = "角色集合")
	private List<String> roles;

	/**
	 * 第三方授权id
	 */
	@Schema(description = "第三方授权id")
	private String oauthId;

}
