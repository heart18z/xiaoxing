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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import lombok.AllArgsConstructor;
import jakarta.validation.Valid;

import org.springblade.core.secure.BladeUser;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.desk.enums.BizScheduledTriggerModeEnum;
import org.springblade.modules.quartz.executor.SyncPeopleExecutor;

import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.modules.system.entity.PeopleEntity;
import org.springblade.modules.system.vo.PeopleVO;
import org.springblade.modules.system.excel.PeopleExcel;
import org.springblade.modules.system.wrapper.PeopleWrapper;
import org.springblade.modules.system.service.IPeopleService;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.tool.constant.BladeConstant;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 人员 控制器
 *
 * @author BladeX
 * @since 2024-01-15
 */
@RestController
@AllArgsConstructor
@RequestMapping("blade-people/people")
@Tag(name = "人员接口", description = "人员")
public class PeopleController extends BladeController {

	private final IPeopleService peopleService;

	private final SyncPeopleExecutor syncPeopleExecutor;

	@PostMapping("/sync")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "同步数据")
	public R doSync() {
		syncPeopleExecutor.doSyncPeopleDataByInterface(BizScheduledTriggerModeEnum.TASK_SYNC_HAND.getType());
		return R.status(true);
	}

	/**
	 * 人员 详情
	 */
	@PostMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入people")
	@PreAuth("hasPermission('blade:people:detail')")
	public R<PeopleVO> detail(PeopleEntity people) {
		PeopleVO detail = peopleService.detail(people.getId());
		return R.data(detail);
	}
	/**
	 * 人员 分页
	 */
	@PostMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入people")
	@PreAuth("hasPermission('blade:people:list')")
	public R<IPage<PeopleVO>> list(@RequestParam Map<String, Object> people, Query query) {
		QueryWrapper<PeopleEntity> queryWrapper;
		if (Func.isNotEmpty(people.get("account"))) {
			String account = people.get("account").toString();
			people.remove("account");
			queryWrapper = Condition.getQueryWrapper(people, PeopleEntity.class);
			queryWrapper.lambda().like(PeopleEntity::getAccount,account);
		}else {
			queryWrapper = Condition.getQueryWrapper(people, PeopleEntity.class);
		}



		IPage<PeopleEntity> pages = peopleService.page(Condition.getPage(query), queryWrapper);
		return R.data(PeopleWrapper.build().pageVO(pages));
	}


	/**
	 * 人员 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入people")
	@PreAuth("hasPermission('blade:people:submit')")
	public R submit(@Valid @RequestBody PeopleVO people) {
		return R.status(peopleService.submit(people));
	}

	/**
	 * 人员 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	@PreAuth("hasPermission('blade:people:remove')")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(peopleService.deleteLogic(Func.toLongList(ids)));
	}

	@PostMapping("/select")
	@ApiOperationSupport(order = 10)
	public R<List<PeopleVO>> select() {
		List<PeopleEntity> list = peopleService.list(new LambdaQueryWrapper<PeopleEntity>().orderByAsc(PeopleEntity::getCreateTime));
		return R.data(PeopleWrapper.build().selectVO(list));
	}

	/**
	 * 获取分页列表
	 *
	 * @param people
	 * @param query
	 * @return R
	 */
	@PostMapping("/listPage")
	public R<IPage<PeopleEntity>> getListPage(PeopleVO people, Query query) {
		return R.data(peopleService.getListPage(Condition.getPage(query), people));
	}

	@PostMapping("peopleListByIds")
	public R getPeopleListByIds(PeopleEntity people) {
		return R.data(peopleService.getPeopleListByIds(people));
	}
}
