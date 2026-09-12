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
package org.springblade.modules.desk.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import org.springblade.modules.auth.utils.TokenUtil;
import org.springblade.modules.desk.service.IBizNoticeService;
import org.springblade.modules.desk.service.IGroupService;
import org.springblade.modules.desk.vo.BizNoticeMessageParamVO;
import org.springblade.modules.desk.vo.BizNoticeMessageVO;
import org.springblade.modules.desk.vo.NoticeIdsVO;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.service.IDeptService;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 公告通知表 控制器
 *
 * @author BladeX
 * @since 2023-07-31
 */
@RestController
@AllArgsConstructor
@RequestMapping("blade-bizNotice/bizNotice")
@Tag(name = "公告通知表接口", description = "公告通知表")
public class BizNoticeController extends BladeController {

	private final IBizNoticeService bizNoticeService;

	private final IGroupService groupService;

	private final IDeptService deptService;

	/**
	 * 批量添加通知
	 */
	@PostMapping("/addNotice")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "批量添加通知", description = "传入notice")
	public R batchAddNotices(@Parameter(description = "添加的通知") @RequestBody NoticeIdsVO notice) {
		return R.status(bizNoticeService.addNotice(notice.getNotice(), notice.getIds(),notice.getAttachSourceList()));
	}

	/**
	 * 更新通知
	 */
	@PostMapping("/updateNotice")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "更新通知", description = "传入notice")
	public R updateNotice(@Parameter(description = "添加的通知") @RequestBody BizNoticeMessageVO noticeMessage) {
		return R.status(bizNoticeService.updateNotice(noticeMessage));
	}

	/*条件发送的消息*/
	@PostMapping("/selectNoticeManage")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "自定义分页查询", description = "传入dataLake")
	public R<IPage<BizNoticeMessageVO>> selectNoticeManage(@RequestBody BizNoticeMessageParamVO noticeCondition, Query query) {
		IPage<BizNoticeMessageVO> listIPage = bizNoticeService.selcectNoticeManage(noticeCondition, Condition.getPage(query));
		return R.data(listIPage);
	}

	/*查询发送人姓名*/
	@PostMapping("/selectSendNameList")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "返回发送人用户名")
	public R<List<User>> selectListName() {
		return R.data(bizNoticeService.selectAllUserName());
	}

	/*查询接收人姓名*/
	@PostMapping("/selectRecipientNameList")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "返回接收人用户名")
	public R<List<User>> selectRecipientName() {
		return R.data(bizNoticeService.selectRecipientName());
	}

	/*条件获取接收的消息*/
	@PostMapping("/getMessageBySendUser")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "自定义分页查询", description = "传入dataLake")
	public R<List<BizNoticeMessageVO>> getMessageBySendUser(@RequestBody BizNoticeMessageParamVO noticeCondition) {
		List<BizNoticeMessageVO> list = bizNoticeService.selcectMessageBySendUser(noticeCondition);
		return R.data(list);
	}

	/**
	 * 获取部门列表树
	 */
	@PostMapping("/groupTree")
	@ApiOperationSupport(order = 11)
	@Operation(summary = "临时接口，获得group树形列表", description = "groupId")
	public R tree() {
		return R.data(groupService.tree());
	}

	/**
	 * 获取部门列表树
	 */
	@PostMapping("/deptTree")
	@ApiOperationSupport(order = 11)
	@Operation(summary = "临时接口，获得dept树形列表", description = "deptId")
	public R deptTree() {
		return R.data(deptService.tree(TokenUtil.DEFAULT_TENANT_ID));
	}

	@PostMapping("/selectGroupUser")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "查询用户", description = "传入分组id")
	public R<List<User>> selectDeptUser(@RequestParam String groupId, @RequestParam String userName) {
		List<User> users = bizNoticeService.selectGroupUser(groupId, userName);
		return R.data(users);
	}

}
