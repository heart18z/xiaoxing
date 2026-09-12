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


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springblade.common.constant.CommonConstant;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.modules.desk.enums.BizScheduledTriggerModeEnum;
import org.springblade.modules.quartz.constants.ScheduleConstants;
import org.springblade.modules.quartz.domain.SysJob;
import org.springblade.modules.quartz.executor.BaseDictExecutor;
import org.springblade.modules.quartz.service.ISysJobLogService;
import org.springblade.modules.quartz.service.ISysJobService;
import org.springblade.modules.quartz.util.CronUtils;
import org.springblade.modules.system.entity.InteractiveEntity;
import org.springblade.modules.system.mapper.BizParamMapper;
import org.springblade.modules.system.service.IBizParamService;
import org.springblade.modules.system.vo.InteractiveVO;
import org.springblade.modules.system.excel.InteractiveExcel;
import org.springblade.modules.system.mapper.InteractiveMapper;
import org.springblade.modules.system.service.IInteractiveService;
import org.springblade.modules.system.wrapper.InteractiveWrapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 对外交互 服务实现类
 *
 * @author BladeX
 * @since 2024-08-20
 */
@Service
@RequiredArgsConstructor(onConstructor_= {@Lazy})
public class InteractiveServiceImpl extends BaseServiceImpl<InteractiveMapper, InteractiveEntity> implements IInteractiveService {

	private final ISysJobService sysJobService;

	@Override
	public IPage<InteractiveVO> selectInteractivePage(IPage<InteractiveEntity> page, Map<String, Object> interactive) {
		IPage<InteractiveEntity> pages = this.page(page , Condition.getQueryWrapper(interactive, InteractiveEntity.class));

		if (pages.getRecords().isEmpty()) {
			return new Page(pages.getCurrent(), pages.getSize(), pages.getTotal());
		}
		Map<Long,List<SysJob>> idJobs = sysJobService.list(new LambdaQueryWrapper<SysJob>()
				.isNotNull(SysJob::getInteractionId))
			.stream().collect(Collectors.groupingBy(SysJob::getInteractionId));
		List<InteractiveVO> records =pages.getRecords().stream().map(i->{
			InteractiveVO vo = BeanUtil.copy(i,InteractiveVO.class);
			List<SysJob> jobs = idJobs.get(vo.getId());
			if (jobs!=null&&(!jobs.isEmpty())){
				vo.setJobsName(jobs.stream().map(job->{
					String code = Func.isNotBlank(job.getJobCode())?job.getJobCode():"";
					String name = Func.isNotBlank(job.getJobName())?job.getJobName():"";
					return code+"_"+name;
				}).collect(Collectors.joining(",")));

				jobs = jobs.stream().map(j->{
					j.setJobGroup("");
					if (ScheduleConstants.Status.NORMAL.getValue().equals(j.getProcessStatus())) {
						List<String> dateList = CronUtils.getRecentTriggerTime(j.getCronExpression());
						if (dateList!=null && !dateList.isEmpty()) {
							j.setJobGroup(dateList.get(0));
						}
					}
					return j;
				}).collect(Collectors.toList());

				vo.setJobList(jobs);
			} else {
				vo.setJobList(Collections.emptyList());
			}
			return vo;
		}).collect(Collectors.toList());

		IPage<InteractiveVO> pageVo = new Page(pages.getCurrent(), pages.getSize(), pages.getTotal());
		pageVo.setRecords(records);
		return pageVo;
	}


	@Override
	public List<InteractiveExcel> exportInteractive(Wrapper<InteractiveEntity> queryWrapper) {
		List<InteractiveExcel> interactiveList = baseMapper.exportInteractive(queryWrapper);
		//interactiveList.forEach(interactive -> {
		//	interactive.setTypeName(DictCache.getValue(DictEnum.YES_NO, Interactive.getType()));
		//});
		return interactiveList;
	}

	@Override
	public Boolean checkTaskPermit(String taskCode) {
		if (Func.isBlank(taskCode)) {
			return false;
		}
		InteractiveEntity interactiveEntity = this.getOne(new LambdaQueryWrapper<InteractiveEntity>()
			.eq(InteractiveEntity::getTaskCode,taskCode)
		);
		if (interactiveEntity==null || CommonConstant.DB_STATUS_NOT_NORMAL ==interactiveEntity.getStatus()) {
			return false;
		}
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submit(InteractiveEntity interactive) {

		if (null!=interactive.getId()) {
			InteractiveEntity db =  this.getById(interactive.getId());
			if (db.getStatus() == CommonConstant.DB_STATUS_NORMAL&&
				interactive.getStatus() ==CommonConstant.DB_STATUS_NOT_NORMAL)
			{
				List<SysJob> sysJobs = sysJobService.list(new LambdaQueryWrapper<SysJob>()
					.eq(SysJob::getInteractionId,interactive.getId())
					.eq(SysJob::getProcessStatus, ScheduleConstants.Status.NORMAL.getValue())
				);
				if (!sysJobs.isEmpty()) {
					throw new ServiceException("关闭对外交互失败，请先关闭相关联的定时任务！");
				}
			}
			if (Func.isNotBlank(interactive.getInteractiveCode())) {
				long count =this.count(new LambdaQueryWrapper<InteractiveEntity>()
					.eq(InteractiveEntity::getInteractiveCode,interactive.getInteractiveCode())
					.ne(InteractiveEntity::getId,interactive.getId())
				);
				if (count>0) {
					throw new ServiceException("对外交互编码不能重复！");
				}
			}
		}else {
			if (Func.isNotBlank(interactive.getInteractiveCode())) {
				long count =this.count(new LambdaQueryWrapper<InteractiveEntity>()
					.eq(InteractiveEntity::getInteractiveCode,interactive.getInteractiveCode())
				);
				if (count>0) {
					throw new ServiceException("对外交互编码不能重复！");
				}
			}
		}

		return this.saveOrUpdate(interactive);
	}

	@Override
	public boolean changeInteractiveStatus(InteractiveEntity interactive) {
		if (null!=interactive.getId()) {
			InteractiveEntity db =  this.getById(interactive.getId());
			if (db.getStatus() == CommonConstant.DB_STATUS_NORMAL&&
				interactive.getStatus() ==CommonConstant.DB_STATUS_NOT_NORMAL)
			{
				List<SysJob> sysJobs = sysJobService.list(new LambdaQueryWrapper<SysJob>()
					.eq(SysJob::getInteractionId,interactive.getId())
					.eq(SysJob::getProcessStatus, ScheduleConstants.Status.NORMAL.getValue())
				);
				if (!sysJobs.isEmpty()) {
					throw new ServiceException("关闭对外交互失败，请先关闭相关联的定时任务！");
				}


			}



		}
		return this.updateById(interactive);
	}

}
