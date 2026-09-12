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
package org.springblade.modules.resource.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

import org.springblade.core.tool.api.R;
import org.springblade.modules.resource.service.IAttachPermissionLogService;
import org.springframework.web.bind.annotation.*;
import org.springblade.modules.resource.vo.AttachPermissionLogVO;
import org.springblade.core.boot.ctrl.BladeController;

import java.util.List;

/**
 * 文件权限修改日志 控制器
 *
 * @author BladeX
 * @since 2024-08-20
 */
@RestController
@AllArgsConstructor
@RequestMapping("blade-attachPermissionLog/attachPermissionLog")
@Tag(name = "文件权限修改日志接口", description = "文件权限修改日志")
public class AttachPermissionLogController extends BladeController {

	private final IAttachPermissionLogService attachPermissionLogService;

	/**
	 * 文件权限修改日志
	 */
	@PostMapping("/list")
	public R<List<AttachPermissionLogVO>> listByAttachSourceId(Long attachSourceId ) {
		List<AttachPermissionLogVO> pages = attachPermissionLogService.listByAttachSource(attachSourceId);
		return R.data(pages);
	}



}
