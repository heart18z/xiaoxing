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

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import org.springblade.common.cache.ParamCache;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.resource.entity.Attach;
import org.springblade.modules.resource.service.IAttachService;
import org.springblade.modules.resource.vo.AttachVO;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 附件表 控制器
 *
 * @author Chill
 */
@NonDS
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_RESOURCE_NAME + "/attach")
@Tag(name = "附件", description = "附件")
public class AttachController extends BladeController {

	private final static String UPLOAD_SYSTEM_PARAM_KEY="project.name";

	private final IAttachService attachService;

	/**
	 * 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入attach")
	public R<Attach> detail(Attach attach) {
		Attach detail = attachService.getOne(Condition.getQueryWrapper(attach));
		return R.data(detail);
	}

	/**
	 * 分页 附件表
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入attach")
	public R<IPage<Attach>> list(Attach attach, Query query) {
		IPage<Attach> pages = attachService.page(Condition.getPage(query), Condition.getQueryWrapper(attach));
		return R.data(pages);
	}

	/**
	 * 自定义分页 附件表
	 */
	@PostMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入attach")
	public R<IPage<AttachVO>> page(AttachVO attach, Query query) {
		IPage<AttachVO> pages = attachService.selectAttachPage(Condition.getPage(query), attach);
		return R.data(pages);
	}

	/**
	 * 新增 附件表
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入attach")
	public R save(@Valid @RequestBody Attach attach) {
		return R.status(attachService.save(attach));
	}

	/**
	 * 修改 附件表
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入attach")
	public R update(@Valid @RequestBody Attach attach) {
		return R.status(attachService.updateById(attach));
	}

	/**
	 * 新增或修改 附件表
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入attach")
	public R submit(@Valid @RequestBody Attach attach) {
		attach.setUploadSystem(ParamCache.getValue(UPLOAD_SYSTEM_PARAM_KEY));
		attachService.saveOrUpdate(attach);
		return R.data(attach);
	}


	/**
	 * 删除 附件表
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(attachService.deleteLogic(Func.toLongList(ids)));
	}


}
