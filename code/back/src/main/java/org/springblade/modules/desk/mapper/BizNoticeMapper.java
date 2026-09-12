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
package org.springblade.modules.desk.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.desk.entity.BizNoticeEntity;
import org.springblade.modules.desk.excel.BizNoticeExcel;
import org.springblade.modules.desk.vo.BizNoticeMessageParamVO;
import org.springblade.modules.desk.vo.BizNoticeMessageVO;
import org.springblade.modules.desk.vo.BizNoticeVO;
import org.springblade.modules.system.entity.User;

import java.util.List;

/**
 * 公告通知表 Mapper 接口
 *
 * @author BladeX
 * @since 2023-07-31
 */
public interface BizNoticeMapper extends BaseMapper<BizNoticeEntity> {

	/**
	 * 自定义分页
	 *
	 * @param page
	 * @param bizNotice
	 * @return
	 */
	List<BizNoticeVO> selectBizNoticePage(IPage page, BizNoticeVO bizNotice);


	/**
	 * 获取导出数据
	 *
	 * @param queryWrapper
	 * @return
	 */
	List<BizNoticeExcel> exportBizNotice(@Param("ew") Wrapper<BizNoticeEntity> queryWrapper);


	List<BizNoticeMessageVO> getMessagePage(IPage page, BizNoticeMessageParamVO noticeCondition);

	List<BizNoticeMessageVO> getUserSendMessage(IPage page, BizNoticeMessageParamVO noticeCondition);

	List<Long> selectListUserId(Long userId);

	List<Long> selectRecipientUserIdList(Long userId);


	List<BizNoticeMessageVO> selcectMessageBySendUser(@Param("noticeCondition")BizNoticeMessageParamVO noticeCondition);

	List<User> selectGroupUser(@Param("groupId")String groupId, @Param("userName")String userName);
}
