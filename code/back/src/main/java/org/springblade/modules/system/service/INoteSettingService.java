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
import org.springblade.core.tool.api.R;
import org.springblade.modules.system.entity.NoteEntity;
import org.springblade.modules.system.entity.NoteSettingEntity;
import org.springblade.modules.system.vo.NoteSettingVO;
import org.springblade.modules.system.excel.NoteSettingExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.support.Query;
import org.springblade.core.mp.base.BaseService;
import org.springblade.modules.system.vo.NoteVO;

import java.util.List;
import java.util.Map;

/**
 * 注释设置 服务类
 *
 * @author BladeX
 * @since 2024-01-17
 */
public interface INoteSettingService extends BaseService<NoteSettingEntity> {
	/**
	 * 自定义分页
	 *
	 * @param page
	 * @param noteSetting
	 * @return
	 */
	IPage<NoteSettingVO> selectNoteSettingPage(IPage<NoteSettingVO> page, NoteSettingVO noteSetting);


	/**
	 * 导出数据
	 *
	 * @param queryWrapper
	 * @return
	 */
	List<NoteSettingExcel> exportNoteSetting(Wrapper<NoteSettingEntity> queryWrapper);

    List<NoteSettingVO> selectDomNote(NoteSettingVO note);

	Boolean submit(NoteSettingEntity noteSetting);
}
