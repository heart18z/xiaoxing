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
import lombok.AllArgsConstructor;
import org.springblade.modules.resource.dto.AttachSourceDTO;
import org.springblade.modules.resource.entity.AttachSourceEntity;
import org.springblade.modules.resource.service.IAttachSourceService;
import org.springblade.modules.resource.vo.AttachSourceVO;
import org.springblade.modules.resource.vo.RegionVo;
import org.springblade.modules.resource.wrapper.AttachSourceWrapper;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import org.springblade.modules.resource.vo.AttachVO;
import org.springblade.modules.resource.vo.FileUpLoadVO;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 附件来源表 控制器
 *
 * @author BladeX
 * @since 2023-06-10
 */
@RestController
@AllArgsConstructor
@RequestMapping("blade-attach-source/attachSource")
@Tag(name = "附件来源表接口", description = "附件来源表")
public class AttachSourceController extends BladeController {

	private final IAttachSourceService attachSourceService;

	//文件保密用户设置角色
	private final String PERMISSION_USER_ROLE_NAME="fileSecrecySet";

	/**
	 * 附件来源表 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入attachSource")
	public R<AttachSourceVO> detail(AttachSourceEntity attachSource) {
		AttachSourceEntity detail = attachSourceService.getOne(Condition.getQueryWrapper(attachSource));
		return R.data(AttachSourceWrapper.build().entityVO(detail));
	}
	/**
	 * 附件来源表 分页
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入attachSource")
	public R<IPage<AttachSourceVO>> list(@RequestParam Map<String, Object> attachSource, Query query) {
		IPage<AttachSourceEntity> pages = attachSourceService.page(Condition.getPage(query), Condition.getQueryWrapper(attachSource, AttachSourceEntity.class));
		return R.data(AttachSourceWrapper.build().pageVO(pages));
	}

	/**
	 * 附件来源表 自定义分页
	 */
	@PostMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入attachSource")
	public R<IPage<AttachSourceVO>> page(AttachSourceVO attachSource, Query query) {
		IPage<AttachSourceVO> pages = attachSourceService.selectAttachSourcePage(Condition.getPage(query), attachSource);
		return R.data(pages);
	}

	/**
	 * 附件来源表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入attachSource")
	public R save(@Valid @RequestBody AttachSourceEntity attachSource) {
		return R.status(attachSourceService.save(attachSource));
	}

	/**
	 * 附件来源表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入attachSource")
	public R update(@Valid @RequestBody AttachSourceEntity attachSource) {
		return R.status(attachSourceService.updateById(attachSource));
	}
//
//	/**
//	 * 附件来源表 新增或修改
//	 */
//	@PostMapping("/submit")
//	@ApiOperationSupport(order = 6)
//	@Operation(summary = "新增或修改", description = "传入attachSource")
//	public R submit(@Valid @RequestBody AttachSourceEntity attachSource) {
//		return R.status(attachSourceService.saveOrUpdate(attachSource));
//	}

	/**
	 * 附件来源表 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@RequestBody AttachSourceDTO attachRemoveVOS) {
		return R.status(attachSourceService.removeAttach(attachRemoveVOS));
	}

	@PostMapping("/post-sign")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "发起签署文件", description = "传入ids")
	public R postSign(@RequestBody AttachSourceDTO attachRemoveVOS) {
		return R.status(attachSourceService.postSign(attachRemoveVOS));
	}

	/**
	 * 根据来源获取附件
	 */
	@PostMapping("/list-by-source")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "", description = "传入attachSource")
	public R<List<AttachVO>> list(FileUpLoadVO fileUpLoadVO) {
		List<AttachVO> list = attachSourceService.getAttachBySource(fileUpLoadVO);
		return R.data(list);
	}


	@PostMapping("/list-by-oriId")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "", description = "传入attachSource")
	public R<List<AttachVO>> listByOriId(FileUpLoadVO fileUpLoadVO) {
		List<AttachVO> list = attachSourceService.listByOriId(fileUpLoadVO);
		return R.data(list);
	}


	/**
	 * 附件来源表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入attachSource")
	public R submit( @RequestBody AttachSourceDTO attachSource) {

		return R.status(attachSourceService.submit(attachSource));
	}

	/**
	 * 修改保密用户
	 * @param attachSource
	 * @return
	 */
	@PostMapping("/user-account-permission")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "修改保密用户", description = "传入attachSource")
	public R userAccountPermission( @RequestBody AttachSourceDTO attachSource) {

		return R.status(attachSourceService.changeUserAccountPermission(attachSource));
	}


	/**
	 * 获取实物保管区域
	 */
	@PostMapping("/region-list")
	@ApiOperationSupport(order = 7)
	public R<List<RegionVo>> list() {
		List<RegionVo> list = attachSourceService.getRegionList();
		return R.data(list);
	}



}
