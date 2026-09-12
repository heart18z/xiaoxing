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
import org.springblade.modules.desk.entity.ScheduledLogEntity;
import org.springblade.modules.desk.excel.ScheduledLogExcel;
import org.springblade.modules.desk.mapper.ScheduledLogMapper;
import org.springblade.modules.desk.service.IScheduledLogService;
import org.springblade.modules.desk.vo.ScheduledLogVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 * 定时任务日志表 服务实现类
 *
 * @author BladeX
 * @since 2024-08-05
 */
@Service
public class ScheduledLogServiceImpl extends BaseServiceImpl<ScheduledLogMapper, ScheduledLogEntity> implements IScheduledLogService {


	@Override
	public IPage<ScheduledLogVO> selectScheduledLogPage(IPage<ScheduledLogVO> page, ScheduledLogVO scheduledLog) {
		return page.setRecords(baseMapper.selectScheduledLogPage(page, scheduledLog));
	}


	@Override
	public List<ScheduledLogExcel> exportScheduledLog(Wrapper<ScheduledLogEntity> queryWrapper) {
		List<ScheduledLogExcel> scheduledLogList = baseMapper.exportScheduledLog(queryWrapper);
		//scheduledLogList.forEach(scheduledLog -> {
		//	scheduledLog.setTypeName(DictCache.getValue(DictEnum.YES_NO, ScheduledLog.getType()));
		//});
		return scheduledLogList;
	}

	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void saveData(ScheduledLogEntity entity) {
		this.save(entity);
	}

}
