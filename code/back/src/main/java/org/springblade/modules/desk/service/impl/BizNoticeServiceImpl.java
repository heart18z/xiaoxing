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
package org.springblade.modules.desk.service.impl;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.AllArgsConstructor;
import org.springblade.modules.resource.entity.AttachSourceEntity;
import org.springblade.modules.resource.service.IAttachSourceService;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.utils.CollectionUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.desk.entity.BizNoticeEntity;
import org.springblade.modules.desk.entity.BizNoticeMessageEntity;
import org.springblade.modules.desk.enums.BizNoticeSendTypeEnum;
import org.springblade.modules.desk.enums.BizNoticeStatusEnum;
import org.springblade.modules.desk.excel.BizNoticeExcel;
import org.springblade.modules.desk.mapper.BizNoticeMapper;
import org.springblade.modules.desk.service.IBizNoticeMessageService;
import org.springblade.modules.desk.service.IBizNoticeService;
import org.springblade.modules.desk.vo.BizNoticeMessageParamVO;
import org.springblade.modules.desk.vo.BizNoticeMessageVO;
import org.springblade.modules.desk.vo.BizNoticeVO;
import org.springblade.modules.resource.vo.AttachVO;
import org.springblade.modules.system.entity.User;
import org.springblade.modules.system.service.IUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 公告通知表 服务实现类
 *
 * @author BladeX
 * @since 2023-07-31
 */
@Service
@AllArgsConstructor
public class BizNoticeServiceImpl extends BaseServiceImpl<BizNoticeMapper, BizNoticeEntity> implements IBizNoticeService {

	private final IBizNoticeMessageService noticeMessageService;

	private final IUserService userService;

	private final IAttachSourceService attachSourceService;

	private final String NOTICE_MESSAGE_STATUS_UNREAD="unread";
	@Override
	public IPage<BizNoticeVO> selectBizNoticePage(IPage<BizNoticeVO> page, BizNoticeVO bizNotice) {
		return page.setRecords(baseMapper.selectBizNoticePage(page, bizNotice));
	}


	@Override
	public List<BizNoticeExcel> exportBizNotice(Wrapper<BizNoticeEntity> queryWrapper) {
		List<BizNoticeExcel> bizNoticeList = baseMapper.exportBizNotice(queryWrapper);
		//bizNoticeList.forEach(bizNotice -> {
		//	bizNotice.setTypeName(DictCache.getValue(DictEnum.YES_NO, BizNotice.getType()));
		//});
		return bizNoticeList;
	}

	@Override
	@Transactional
	public Boolean addNotice(BizNoticeEntity notice, List<Long> ids,List<AttachVO> attachSourceList) {
		if (ids == null || ids.size() == 0) {
			throw new ServiceException("请选择通知发送的目标用户！");
		}
		Date date = new Date();
		// 设置直接发送
		Long id = IdWorker.getId(notice);
		notice.setId(id);
		notice.setNoticeBuildTime(date);
		notice.setNoticeStatus(BizNoticeStatusEnum.NOTICE_SEND.getCategory());
		notice.setNoticeSentTime(date);
		notice.setIsShow(1);
		notice.setSendType(BizNoticeSendTypeEnum.SEND_IMMEDIATELY.getCategory());
		notice.setCreateTime(date);
		notice.setStatus(1);
		notice.setCreateUser(AuthUtil.getUserId());
		this.save(notice);
		if (attachSourceList !=null) {
			this.insertAttach(attachSourceList,id);
		}
		// 设置接收人员，创建message
		List<BizNoticeMessageEntity> messages = messageBuild(notice,ids);
		noticeMessageService.saveBatch(messages);
		return true;
	}
	private void insertAttach(List<AttachVO> attachSources,Long sourceId) {
		if (attachSources ==null || attachSources.size() == 0) {
			return;
		}
		List<AttachSourceEntity> attachSourceEntitys = new ArrayList<>();
		for(AttachVO attachVO : attachSources) {
			AttachSourceEntity attachSourceEntity = new AttachSourceEntity();
			attachSourceEntity.setDiyFileName(attachVO.getDiyFileName());
			attachSourceEntity.setAttachExtension(attachVO.getAttachExtension());
			attachSourceEntity.setAttachId(attachVO.getId());
			attachSourceEntity.setSourceId(sourceId);
			attachSourceEntity.setSourceType(2);
			attachSourceEntitys.add(attachSourceEntity);

		}
		attachSourceService.saveBatch(attachSourceEntitys);
	}

	@Override
	public IPage<BizNoticeMessageVO> selcectNoticeManage(BizNoticeMessageParamVO noticeCondition, IPage<BizNoticeMessageVO> page) {
		List<BizNoticeMessageVO> noticeMessageVOList = new ArrayList<>();
		if (noticeCondition.getIsRecipient()) {
			noticeMessageVOList =   getUserRecipientMessage(noticeCondition,page);
		}else {
			noticeMessageVOList =   getUserSendMessage(noticeCondition,page);
		}
		page.setRecords(noticeMessageVOList);
		return page;
	}

	@Override
	public List<User> selectAllUserName() {
		Long userId = AuthUtil.getUserId();
		List<Long> userIds = baseMapper.selectListUserId(userId);
		List<User> result = new ArrayList<>();
		if (userIds.size() >0) {
			List<User> users = userService.list(new LambdaQueryWrapper<User>()
				.in(User::getId,userIds));
			for (User user: users) {
				User item = new User();
				item.setId(user.getId());
				item.setName(user.getName());
				result.add(item);
			}
			return result;
		}else {
			return new ArrayList<>();
		}
	}

	@Override
	public List<User> selectRecipientName() {
		Long userId = AuthUtil.getUserId();
		List<Long> userIds = baseMapper.selectRecipientUserIdList(userId);
		List<User> result = new ArrayList<>();
		if (userIds.size() >0) {
			List<User> users = userService.list(new LambdaQueryWrapper<User>()
				.in(User::getId,userIds));
			for (User user: users) {
				User item = new User();
				item.setId(user.getId());
				item.setName(user.getName());
				result.add(item);
			}
			return result;
		}else {
			return new ArrayList<>();
		}
	}

	@Override
	public List<BizNoticeMessageVO> selcectMessageBySendUser(BizNoticeMessageParamVO noticeCondition) {
		return this.baseMapper.selcectMessageBySendUser(noticeCondition);
	}

	@Override
	public Boolean updateNotice(BizNoticeMessageVO noticeMessage) {
		if (noticeMessage.getId() == null) {
			throw new ServiceException("消息不存在");
		}
		BizNoticeMessageEntity systemNotice = noticeMessageService.getById(noticeMessage.getId());
		if (systemNotice == null) {
			throw new ServiceException("消息不存在");
		}
		Date now = new Date();
		if (Func.isNotBlank(noticeMessage.getNoticeReadStatus())) {
			BizNoticeMessageEntity messageEntity = new BizNoticeMessageEntity();
			messageEntity.setId(noticeMessage.getId());
			if (NOTICE_MESSAGE_STATUS_UNREAD.equals(noticeMessage.getNoticeReadStatus())
				&& systemNotice.getNoticeReadStatus().equals(noticeMessage.getNoticeReadStatus())) {
				return true;
			}else {
				messageEntity.setNoticeReadStatus(noticeMessage.getNoticeReadStatus());
				messageEntity.setNoticeReply(noticeMessage.getNoticeReply());
				messageEntity.setNoticeReadTime(now);
				messageEntity.setNoticeReplyTime(now);
				messageEntity.setUpdateTime(now);
				noticeMessageService.updateById(messageEntity);
			}
		}
		return true;
	}

	@Override
	public List<User> selectGroupUser(String groupId, String userName) {
		return baseMapper.selectGroupUser(groupId,userName);
	}

	private List<BizNoticeMessageVO> getUserRecipientMessage(BizNoticeMessageParamVO noticeCondition, IPage<BizNoticeMessageVO> page) {
		noticeCondition.setReceiveUserId(AuthUtil.getUserId());
		return this.baseMapper.getMessagePage(page,noticeCondition);
	}

	private List<BizNoticeMessageVO> getUserSendMessage(BizNoticeMessageParamVO noticeCondition, IPage<BizNoticeMessageVO> page) {

		String userRole = AuthUtil.getUserRole();
		if (!CollectionUtil.contains(Func.toStrArray(userRole), RoleConstant.ADMIN) && !CollectionUtil.contains(Func.toStrArray(userRole), RoleConstant.ADMINISTRATOR)) {
			noticeCondition.setSendUserId(AuthUtil.getUserId());
		}
		return this.baseMapper.getUserSendMessage(page,noticeCondition);
	}

	private List<BizNoticeMessageEntity> messageBuild(BizNoticeEntity bizNotice, List<Long> ids){
		List<BizNoticeMessageEntity> messageEntities = new ArrayList<>();
		for(Long id: ids) {
			BizNoticeMessageEntity message = new BizNoticeMessageEntity();
			message.setNoticeId(bizNotice.getId());
			message.setNoticeReadStatus(NOTICE_MESSAGE_STATUS_UNREAD);
			message.setReceiveUserId(id);
			message.setSendUserId(bizNotice.getCreateUser());
			messageEntities.add(message);
		}
		return messageEntities;
	}

}
