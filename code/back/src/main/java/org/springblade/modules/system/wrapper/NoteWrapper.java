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
package org.springblade.modules.system.wrapper;

import org.springblade.common.cache.DictBizCache;
import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.modules.system.entity.DictBiz;
import org.springblade.modules.system.entity.NoteEntity;
import org.springblade.modules.system.vo.NoteVO;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 注释 包装类,返回视图层所需的字段
 *
 * @author BladeX
 * @since 2024-01-17
 */
public class NoteWrapper extends BaseEntityWrapper<NoteEntity, NoteVO>  {

	public static final String NOTE_TYPE_KEY="note_type";

	public static NoteWrapper build() {
		return new NoteWrapper();
 	}

	@Override
	public NoteVO entityVO(NoteEntity note) {
		NoteVO noteVO = Objects.requireNonNull(BeanUtil.copy(note, NoteVO.class));

		//User createUser = UserCache.getUser(note.getCreateUser());
		//User updateUser = UserCache.getUser(note.getUpdateUser());
		//noteVO.setCreateUserName(createUser.getName());
		//noteVO.setUpdateUserName(updateUser.getName());

		return noteVO;
	}

	public List<NoteVO> listVO(List<NoteEntity> list) {
		Map<String,String> dictBizs = DictBizCache.getList(NOTE_TYPE_KEY)
			.stream().collect(Collectors.toMap(DictBiz::getDictKey,DictBiz::getDictValue));

 		 return list.stream().map(this::entityVO)
			 .map(m->{
				 m.setNoteTypeName(dictBizs.get(m.getType()));
				 return m;
			 })
			.collect(Collectors.toList());
	}



}
