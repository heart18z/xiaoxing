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
package org.springblade.modules.system.service.impl;


import lombok.RequiredArgsConstructor;
import org.springblade.modules.quartz.task.BackStageAiTask;
import org.springblade.modules.system.entity.RemoteParamEntity;
import org.springblade.modules.system.vo.RemoteParamVO;
import org.springblade.modules.system.excel.RemoteParamExcel;
import org.springblade.modules.system.mapper.RemoteParamMapper;
import org.springblade.modules.system.service.RemoteParamService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 * ai专用应用字典
 服务实现类
 *
 * @author BladeX
 * @since 2025-03-18
 */
@Service
@RequiredArgsConstructor(onConstructor_= {@Lazy})
public class RemoteParamServiceImpl extends BaseServiceImpl<RemoteParamMapper, RemoteParamEntity> implements RemoteParamService {

	private final BackStageAiTask backStageAiTask;

	@Override
	public IPage<RemoteParamVO> selectRemoteParamPage(IPage<RemoteParamVO> page, RemoteParamVO remoteParam) {
		return page.setRecords(baseMapper.selectRemoteParamPage(page, remoteParam));
	}


	@Override
	public List<RemoteParamExcel> exportRemoteParam(Wrapper<RemoteParamEntity> queryWrapper) {
		List<RemoteParamExcel> remoteParamList = baseMapper.exportRemoteParam(queryWrapper);
		//remoteParamList.forEach(remoteParam -> {
		//	remoteParam.setTypeName(DictCache.getValue(DictEnum.YES_NO, RemoteParam.getType()));
		//});
		return remoteParamList;
	}

	@Override
	public void cleanAllSyncData() {
		baseMapper.deleteAllData();
	}

	@Override
	// 在B中方法上添加注解，另开一个事务
	@Transactional(propagation = Propagation.REQUIRES_NEW,rollbackFor = Exception.class)
	public void doSyncAiParam(Boolean checkRemote) {
//		try {
//			backStageAiTask.syncAiParam(checkRemote);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		backStageAiTask.syncAiParam(checkRemote);

	}

}
