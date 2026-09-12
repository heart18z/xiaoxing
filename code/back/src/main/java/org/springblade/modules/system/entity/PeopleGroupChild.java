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

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户组子表 实体类
 *
 * @author BladeX
 * @since 2024-11-14
 */
@Data
@TableName("blade_people_group_child")
@Schema(name = "GroupChild对象", description = "用户组子表")
@EqualsAndHashCode
public class PeopleGroupChild implements Serializable {

	@TableId(value = "id", type = IdType.ASSIGN_ID)
	private Long id;

	/**
	 * 用户组id
	 */
	@Schema(description = "用户组id")
	private Long peopleGroupId;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long peopleId;

}
