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
package org.springblade.modules.system.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import lombok.AllArgsConstructor;
import jakarta.validation.Valid;

import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.standard.support.vo.KeyValueVO;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.modules.system.entity.InteractiveEntity;
import org.springblade.modules.system.vo.InteractiveVO;
import org.springblade.modules.system.wrapper.InteractiveWrapper;
import org.springblade.modules.system.service.IInteractiveService;
import org.springblade.core.boot.ctrl.BladeController;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对外交互 控制器
 *
 * @author BladeX
 * @since 2024-08-20
 */
@RestController
@AllArgsConstructor
@RequestMapping("blade-interactive/interactive")
@Tag(name = "对外交互接口", description = "对外交互")
public class InteractiveController extends BladeController {

	private final IInteractiveService interactiveService;

	/**
	 * 对外交互 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入interactive")
	public R<InteractiveVO> detail(InteractiveEntity interactive) {
		InteractiveEntity detail = interactiveService.getOne(Condition.getQueryWrapper(interactive));
		return R.data(InteractiveWrapper.build().entityVO(detail));
	}
	/**
	 * 对外交互 分页
	 */
	@PostMapping("/page")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入interactive")
	public R<IPage<InteractiveVO>> list(@RequestParam Map<String, Object> interactive, Query query) {
		IPage<InteractiveEntity> pages = interactiveService.page(Condition.getPage(query), Condition.getQueryWrapper(interactive, InteractiveEntity.class));
		return R.data(InteractiveWrapper.build().pageVO(pages));
	}

	/**
	 * 对外交互 自定义分页
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入interactive")
	public R<IPage<InteractiveVO>> page(@RequestParam Map<String, Object> interactive, Query query) {
		IPage<InteractiveVO> pages = interactiveService.selectInteractivePage(Condition.getPage(query), interactive);
		return R.data(pages);
	}

	/**
	 * 对外交互 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入interactive")
	public R save(@Valid @RequestBody InteractiveEntity interactive) {
		return R.status(interactiveService.save(interactive));
	}

	/**
	 * 对外交互 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入interactive")
	public R update(@Valid @RequestBody InteractiveEntity interactive) {
		return R.status(interactiveService.updateById(interactive));
	}

	/**
	 * 对外交互 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入interactive")
	public R submit(@Valid @RequestBody InteractiveEntity interactive) {
		return R.status(interactiveService.submit(interactive));
	}

	/**
	 * 对外交互 新增或修改
	 */
	@PostMapping("/change-status")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入interactive")
	public R changeStatus(@Valid @RequestBody InteractiveEntity interactive) {
		return R.status(interactiveService.changeInteractiveStatus(interactive));
	}


	/**
	 * 对外交互 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(interactiveService.deleteLogic(Func.toLongList(ids)));
	}


	/**
	 * 对外交互选择任务名称
	 */

	@PostMapping("/dict")
	public R<List<KeyValueVO>> dict() {
		List<KeyValueVO> kvList = interactiveService.list().stream().map(i->
			new KeyValueVO(i.getInteractiveName(),i.getId().toString())
		).collect(Collectors.toList());
		return R.data(kvList);
	}

}
