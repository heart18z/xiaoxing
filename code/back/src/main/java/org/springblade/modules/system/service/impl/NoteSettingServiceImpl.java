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
import lombok.AllArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.modules.system.entity.NoteEntity;
import org.springblade.modules.system.entity.NoteSettingEntity;
import org.springblade.modules.system.service.INoteService;
import org.springblade.modules.system.vo.NoteSettingVO;
import org.springblade.modules.system.excel.NoteSettingExcel;
import org.springblade.modules.system.mapper.NoteSettingMapper;
import org.springblade.modules.system.service.INoteSettingService;
import org.springblade.modules.system.vo.NoteVO;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.mp.base.BaseServiceImpl;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 注释设置 服务实现类
 *
 * @author BladeX
 * @since 2024-01-17
 */
@Service
@AllArgsConstructor
public class NoteSettingServiceImpl extends BaseServiceImpl<NoteSettingMapper, NoteSettingEntity> implements INoteSettingService {


	private final INoteService noteService;

	@Override
	public IPage<NoteSettingVO> selectNoteSettingPage(IPage<NoteSettingVO> page, NoteSettingVO noteSetting) {
		return page.setRecords(baseMapper.selectNoteSettingPage(page, noteSetting));
	}


	@Override
	public List<NoteSettingExcel> exportNoteSetting(Wrapper<NoteSettingEntity> queryWrapper) {
		List<NoteSettingExcel> noteSettingList = baseMapper.exportNoteSetting(queryWrapper);
		//noteSettingList.forEach(noteSetting -> {
		//	noteSetting.setTypeName(DictCache.getValue(DictEnum.YES_NO, NoteSetting.getType()));
		//});
		return noteSettingList;
	}

	@Override
	public List<NoteSettingVO> selectDomNote(NoteSettingVO note) {
		List<NoteSettingVO> res = new ArrayList<>();
//		if (Func.isEmpty(note.getMenuId())) {
//			return res;
//		}
		List<NoteSettingEntity> entityList = this.list(
			new LambdaQueryWrapper<NoteSettingEntity>()
				.eq(Func.isNotEmpty(note.getMenuId()),NoteSettingEntity::getMenuId,note.getMenuId())
		);
		if (entityList.size()>0) {
			Map<Long,NoteEntity> map = noteService.list(new LambdaQueryWrapper<NoteEntity>()
				.in(NoteEntity::getId,entityList.stream().map(NoteSettingEntity::getNoteId).collect(Collectors.toList())))
				.stream().collect(Collectors.toMap(NoteEntity::getId,m->m,(m1,m2)->m1));
			res = entityList.stream().map(m->{
				NoteSettingVO vo = Objects.requireNonNull(BeanUtil.copy(m, NoteSettingVO.class));
				NoteEntity noteEntity = map.get(vo.getNoteId());
				if (Func.isNotEmpty(noteEntity)) {
					vo.setTips(m.getContent());
					vo.setIcon(noteEntity.getIcon());
//					vo.setType(noteEntity.getType());
				}
				return vo;
			}).filter(m->{
				if (Func.isBlank(note.getType())) {
					return true;
				}else if (note.getType().equals(m.getPropLocation())) {return true;}
				else if ("page_default".equals(m.getPropLocation())) {return true;}
				return false;
			}).collect(Collectors.toList());
		}

		return res;
	}

	@Override
	public Boolean submit(NoteSettingEntity noteSetting) {
//		long count = this.count(new LambdaQueryWrapper<NoteSettingEntity>()
//			.eq(NoteSettingEntity::getPropName,noteSetting.getPropName())
//			.eq(N));
		if (Func.isBlank(noteSetting.getPropName()) || Func.isBlank(noteSetting.getPropLocation())) {
			throw new ServiceException("参数不能为空");
		}
		List<String> locations = Arrays.asList(noteSetting.getPropLocation().split(",")).stream().collect(Collectors.toList());;
		List<NoteSettingEntity> settingEntities = this.list(new LambdaQueryWrapper<NoteSettingEntity>()
			.eq(NoteSettingEntity::getPropName,noteSetting.getPropName())
			.ne(noteSetting.getId()!=null,NoteSettingEntity::getId,noteSetting.getId()));
		if (settingEntities.size()>0) {
			//List<String> locationDb = new ArrayList<>();

			for ( NoteSettingEntity item : settingEntities) {
				if(Func.isNotBlank(item.getPropLocation())) {
					List<String> stringList =Arrays.asList(item.getPropLocation().split(","));
					locations.addAll(stringList);
				}
			}
			// 根据元素分组，并统计每个元素的重复次数
			Map<String, Long> countMap = locations.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));

			// 从map中选择具有最高重复次数的元素及其重复次数
			Optional<Map.Entry<String, Long>> maxCountOptional = countMap.entrySet().stream()
				.max(Comparator.comparing(Map.Entry::getValue));

			if (maxCountOptional.isPresent()) {
				if (maxCountOptional.get().getValue()>2) {
					throw new ServiceException("每个字段名称不得配置超过两个相同的显示位置！");
				}
			} else {
				System.out.println("没有任何重复元素");
			}

		}

		return this.saveOrUpdate(noteSetting);
	}

}
