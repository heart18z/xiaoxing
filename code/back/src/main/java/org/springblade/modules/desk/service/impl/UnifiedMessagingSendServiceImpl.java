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


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.AllArgsConstructor;
import org.springblade.modules.resource.dto.AttachSourceDTO;
import org.springblade.modules.resource.service.IAttachSourceService;
import org.springblade.common.constant.MessageConstant;

import org.springblade.common.rpc.support.MessageSystemData;
import org.springblade.common.rpc.support.MessageSystemNotice;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.modules.desk.entity.UnifiedMessagingSendEntity;
import org.springblade.modules.desk.vo.UnifiedMessagingSendVO;
import org.springblade.modules.desk.excel.UnifiedMessagingSendExcel;
import org.springblade.modules.desk.mapper.UnifiedMessagingSendMapper;
import org.springblade.modules.desk.service.IUnifiedMessagingSendService;
import org.springblade.modules.desk.wrapper.UnifiedMessagingSendWrapper;
import org.springblade.modules.resource.vo.AttachVO;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 统一消息发送表 服务实现类
 *
 * @author BladeX
 * @since 2023-12-11
 */
@Service
@AllArgsConstructor
public class UnifiedMessagingSendServiceImpl extends BaseServiceImpl<UnifiedMessagingSendMapper, UnifiedMessagingSendEntity> implements IUnifiedMessagingSendService {

	private final IAttachSourceService attachSourceService;

	private final static String FROM_ID_KEY = "fromId_in";

	@Override
	public IPage<UnifiedMessagingSendVO> selectUnifiedMessagingSendPage(IPage<UnifiedMessagingSendVO> page, UnifiedMessagingSendVO unifiedMessagingSend) {
		return page.setRecords(baseMapper.selectUnifiedMessagingSendPage(page, unifiedMessagingSend));
	}


	@Override
	public List<UnifiedMessagingSendExcel> exportUnifiedMessagingSend(Wrapper<UnifiedMessagingSendEntity> queryWrapper) {
		List<UnifiedMessagingSendExcel> unifiedMessagingSendList = baseMapper.exportUnifiedMessagingSend(queryWrapper);
		//unifiedMessagingSendList.forEach(unifiedMessagingSend -> {
		//	unifiedMessagingSend.setTypeName(DictCache.getValue(DictEnum.YES_NO, UnifiedMessagingSend.getType()));
		//});
		return unifiedMessagingSendList;
	}

	@Override
	@Transactional
	public Boolean submit(UnifiedMessagingSendVO unifiedMessagingSend) {

//		UnifiedMessagingSendEntity entity = Func.copy(unifiedMessagingSend, UnifiedMessagingSendEntity.class);
//		Long id = IdWorker.getId(unifiedMessagingSend);
//		entity.setId(id);
//		if (Func.isNotEmpty(unifiedMessagingSend.getAttachList())) {
//			for (AttachVO attach : unifiedMessagingSend.getAttachList()) {
//				attach.setSourceId(id);
//			}
//			AttachSourceDTO attachSourceDTO = new AttachSourceDTO();
//			attachSourceDTO.setAttachSourceList(unifiedMessagingSend.getAttachList());
//			attachSourceService.submit(attachSourceDTO);
//		}
		if (Func.isNotEmpty(unifiedMessagingSend.getAccountList())) {
			unifiedMessagingSend.setTargetAccount(String.join(",", unifiedMessagingSend.getAccountList()));
		}
		if (Func.isNotEmpty(unifiedMessagingSend.getNameList())) {
			unifiedMessagingSend.setTargetName(String.join(",", unifiedMessagingSend.getNameList()));
		}
		List<UnifiedMessagingSendEntity> adds = new ArrayList<>();
		if(!MessageConstant.MESSAGE_RATE_DATE_NOW.equals(unifiedMessagingSend.getSendRate())) {
			if (unifiedMessagingSend.getEndSentTime()!=null && unifiedMessagingSend.getEndSentTime().getTime()<new Date().getTime()) {
				throw new ServiceException("截止时间小于当前时间，操作失败！");
			}
		}

		if (Func.isNotEmpty(unifiedMessagingSend.getFromIdList())) {
//			entity.setFromId(String.join(",", unifiedMessagingSend.getFromIdList()));
			for (String fromId : unifiedMessagingSend.getFromIdList()) {
				Long id = IdWorker.getId(unifiedMessagingSend);
				UnifiedMessagingSendEntity entity = Func.copy(unifiedMessagingSend, UnifiedMessagingSendEntity.class);
				entity.setId(id);
				entity.setFromId(fromId);
				entity.setStatus(BladeConstant.DB_STATUS_NORMAL);
				entity.setNoticeStatus(MessageConstant.MESSAGE_STATUS_OPEN);
				entity.setCreateUser(AuthUtil.getUserId());
				entity.setCreateTime(DateUtil.now());
				if (MessageConstant.MESSAGE_RATE_DATE_NOW.equals(unifiedMessagingSend.getSendRate())) {
					entity.setSentTime(DateUtil.now());
					entity.setEndSentTime(DateUtil.now());
				}
//				else {
//					entity.setSentTime(unifiedMessagingSend.getSentTime());
//					entity.setEndSentTime(unifiedMessagingSend.getEndSentTime());
//				}
				if (Func.isNotEmpty(unifiedMessagingSend.getAttachList())) {
					for (AttachVO attach : unifiedMessagingSend.getAttachList()) {
						attach.setSourceId(id);
					}
					AttachSourceDTO attachSourceDTO = new AttachSourceDTO();
					attachSourceDTO.setAttachSourceList(unifiedMessagingSend.getAttachList());
					attachSourceService.submit(attachSourceDTO);
				}
				if(Func.isNotBlank(unifiedMessagingSend.getBaseUrl())){
					entity.setHref(unifiedMessagingSend.getBaseUrl()+"?fromId="+fromId);
				}



				MessageSystemData messageSystemData = buildMessageData(entity,unifiedMessagingSend.getAttachList(),unifiedMessagingSend.getAccountList());
				//R res = UnifiedMessageRpcUtils.sendMessage(messageSystemData);
//				if (res!=null) {
//					entity.setMessageSystemId(res.getData()!=null?res.getData().toString():null);
//					entity.setMessageSystemHttpCode(res.getCode());
//				}
				adds.add(entity);
			}

		}

		return this.saveBatch(adds);
	}

	private MessageSystemData buildMessageData(UnifiedMessagingSendEntity entity,List<AttachVO> attachVOS,List<String> accountList) {
		MessageSystemNotice notice = new MessageSystemNotice(){{
			setNoticeTitle(entity.getNoticeTitle());
			setHref(entity.getHref());
			setCategory(entity.getSendType());
			setNoticeType(entity.getSendRate());
			setNoticeContent(entity.getNoticeContent());
//			setTarget(entity.getTargetName());
			setNoticeSentTimeRange(new ArrayList<String>(){{

				add(DateUtil.format(entity.getSentTime(), DateUtil.PATTERN_DATETIME));
				add(DateUtil.format(entity.getEndSentTime(),DateUtil.PATTERN_DATETIME));}});
		}};

		MessageSystemData messageSystemData = new MessageSystemData();
		messageSystemData.setUsers(accountList);
		messageSystemData.setNotice(notice);
		messageSystemData.setAttachList(attachVOS);
		return messageSystemData;
	}

	@Override
	public IPage<UnifiedMessagingSendVO> pageQuery(Query query, Map<String, Object> unifiedMessagingSend) {

		Object fromId = unifiedMessagingSend.get(FROM_ID_KEY);
		unifiedMessagingSend.remove(FROM_ID_KEY);
		QueryWrapper<UnifiedMessagingSendEntity> queryWrapper = Condition.getQueryWrapper(unifiedMessagingSend, UnifiedMessagingSendEntity.class);
		if (Func.isNotEmpty(fromId)) {
			queryWrapper.lambda().in(UnifiedMessagingSendEntity::getFromId, Func.toStrList(fromId.toString()));
		}

		IPage<UnifiedMessagingSendEntity> pages = this.page(Condition.getPage(query), queryWrapper);
		return UnifiedMessagingSendWrapper.build().pageVO(pages);
	}

	@Override
	@Transactional
	public Boolean changeMessageStatus(UnifiedMessagingSendVO unifiedMessagingSend) {
		UnifiedMessagingSendEntity entity = new UnifiedMessagingSendEntity();
		entity.setNoticeStatus(unifiedMessagingSend.getNoticeStatus());
		entity.setId(unifiedMessagingSend.getId());
		if(!MessageConstant.MESSAGE_RATE_DATE_NOW.equals(unifiedMessagingSend.getSendRate())) {
			if (unifiedMessagingSend.getEndSentTime()!=null && unifiedMessagingSend.getEndSentTime().getTime()<new Date().getTime()) {
				throw new ServiceException("截止时间小于当前时间，操作失败！");
			}
		}
		this.updateById(entity);
		//UnifiedMessageRpcUtils.changeStatus(unifiedMessagingSend);
		return  true;
	}

}
