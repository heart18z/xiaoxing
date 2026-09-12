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
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.resource.entity.Attach;
import org.springblade.modules.resource.service.IAttachService;
import org.springblade.modules.resource.vo.AttachVO;
import org.springblade.modules.standard.support.vo.KeyValueVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 附件回收站 控制器
 *
 * @author Chill
 */
@NonDS
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_RESOURCE_NAME + "/attach-recovery")
@Tag(name = "文件回收", description = "文件回收")
public class AttachRecoveryController extends BladeController {

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
	 * 自定义分页 附件表
	 */
	@PostMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入attach")
	@PreAuth("hasPermission('blade:attach-recovery:page')")
	public R<IPage<AttachVO>> page(AttachVO attach, Query query) {
		IPage<AttachVO> pages = attachService.selectAttachPage(Condition.getPage(query), attach);
		return R.data(pages);
	}

	/**
	 * 自定义分页 附件表
	 */
	@PostMapping("/page-of-my")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入attach")
	public R<IPage<AttachVO>> pageByUser(AttachVO attach, Query query) {
		attach.setCreateUser(AuthUtil.getUserId());
		IPage<AttachVO> pages = attachService.selectAttachPage(Condition.getPage(query), attach);
		return R.data(pages);
	}

	/**
	 * 自定义分页 附件表
	 */
	@PostMapping("/page-of-source")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入attach")
	public R<IPage<AttachVO>> pageBySource(AttachVO attach, Query query) {
		IPage<AttachVO> pages = attachService.selectAttachPageBySource(Condition.getPage(query), attach);
		return R.data(pages);
	}

	/**
	 * 删除 附件表
	 */
	@PostMapping("/completeRemove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	@PreAuth("hasPermission('blade:attach-recovery:completeRemove')")
	public R completeRemove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(attachService.completeRemove(Func.toLongList(ids)));
	}

	/**
	 * 删除 附件表
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	@PreAuth("hasPermission('blade:attach-recovery:remove')")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(attachService.removeAttach(Func.toLongList(ids)));
	}

	/**
	 * 恢复附件表
	 */
	@PostMapping("/restore")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "恢复附件", description = "传入ids")
	@PreAuth("hasPermission('blade:attach-recovery:restore')")
	public R restore(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(attachService.restore(Func.toLongList(ids)));
	}

	/**
	 * 获取用户姓名和id的对应kv列表
	 */
	@PostMapping("/oss-bucket-name-list")
	@ApiOperationSupport(order = 20)
	@Operation(summary = "列表")
	public R<List<KeyValueVO>> ossBucketNameList() {
		return R.data(attachService.getBucketNameList());
	}


}
