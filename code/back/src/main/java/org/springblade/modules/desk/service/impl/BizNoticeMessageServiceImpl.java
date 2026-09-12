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
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.modules.desk.entity.BizNoticeMessageEntity;
import org.springblade.modules.desk.excel.BizNoticeMessageExcel;
import org.springblade.modules.desk.mapper.BizNoticeMessageMapper;
import org.springblade.modules.desk.service.IBizNoticeMessageService;
import org.springblade.modules.desk.vo.BizNoticeMessageVO;
import org.springframework.stereotype.Service;

import java.util.List;
/**
 * 公告通知消息表 服务实现类
 *
 * @author BladeX
 * @since 2023-07-31
 */
@Service
public class BizNoticeMessageServiceImpl extends BaseServiceImpl<BizNoticeMessageMapper, BizNoticeMessageEntity> implements IBizNoticeMessageService {


	@Override
	public IPage<BizNoticeMessageVO> selectBizNoticeMessagePage(IPage<BizNoticeMessageVO> page, BizNoticeMessageVO bizNoticeMessage) {
		return page.setRecords(baseMapper.selectBizNoticeMessagePage(page, bizNoticeMessage));
	}


	@Override
	public List<BizNoticeMessageExcel> exportBizNoticeMessage(Wrapper<BizNoticeMessageEntity> queryWrapper) {
		List<BizNoticeMessageExcel> bizNoticeMessageList = baseMapper.exportBizNoticeMessage(queryWrapper);
		//bizNoticeMessageList.forEach(bizNoticeMessage -> {
		//	bizNoticeMessage.setTypeName(DictCache.getValue(DictEnum.YES_NO, BizNoticeMessage.getType()));
		//});
		return bizNoticeMessageList;
	}

}
