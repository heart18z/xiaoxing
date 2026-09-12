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
package org.springblade.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.springblade.modules.system.entity.InteractiveEntity;
import org.springblade.modules.system.vo.InteractiveVO;
import org.springblade.modules.system.excel.InteractiveExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.support.Query;
import org.springblade.core.mp.base.BaseService;
import java.util.List;
import java.util.Map;

/**
 * 对外交互 服务类
 *
 * @author BladeX
 * @since 2024-08-20
 */
public interface IInteractiveService extends BaseService<InteractiveEntity> {
	/**
	 * 自定义分页
	 *
	 * @param page
	 * @param interactive
	 * @return
	 */
	IPage<InteractiveVO> selectInteractivePage(IPage<InteractiveEntity> page, Map<String, Object>  interactive);


	/**
	 * 导出数据
	 *
	 * @param queryWrapper
	 * @return
	 */
	List<InteractiveExcel> exportInteractive(Wrapper<InteractiveEntity> queryWrapper);


	Boolean checkTaskPermit(String taskCode);

    boolean submit(InteractiveEntity interactive);

	boolean changeInteractiveStatus(InteractiveEntity interactive);
}
