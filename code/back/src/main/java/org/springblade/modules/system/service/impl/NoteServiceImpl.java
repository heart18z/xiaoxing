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


import org.springblade.core.tool.api.R;
import org.springblade.modules.system.entity.NoteEntity;
import org.springblade.modules.system.vo.NoteVO;
import org.springblade.modules.system.excel.NoteExcel;
import org.springblade.modules.system.mapper.NoteMapper;
import org.springblade.modules.system.service.INoteService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.mp.base.BaseServiceImpl;
import java.util.List;
/**
 * 注释 服务实现类
 *
 * @author BladeX
 * @since 2024-01-17
 */
@Service
public class NoteServiceImpl extends BaseServiceImpl<NoteMapper, NoteEntity> implements INoteService {


	@Override
	public IPage<NoteVO> selectNotePage(IPage<NoteVO> page, NoteVO note) {
		return page.setRecords(baseMapper.selectNotePage(page, note));
	}


	@Override
	public List<NoteExcel> exportNote(Wrapper<NoteEntity> queryWrapper) {
		List<NoteExcel> noteList = baseMapper.exportNote(queryWrapper);
		//noteList.forEach(note -> {
		//	note.setTypeName(DictCache.getValue(DictEnum.YES_NO, Note.getType()));
		//});
		return noteList;
	}


}
