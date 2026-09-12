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
package org.springblade.modules.standard.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.node.ForestNodeMerger;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.standard.entity.StandardEntity;
import org.springblade.modules.standard.excel.StandardExcel;
import org.springblade.modules.standard.mapper.StandardMapper;
import org.springblade.modules.standard.service.IStandardService;
import org.springblade.modules.standard.vo.StandardNodeVO;
import org.springblade.modules.standard.vo.StandardVO;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 规范类列表 服务实现类
 *
 * @author linchaofan
 * @since 2023-06-09
 */
@Service
public class StandardServiceImpl extends BaseServiceImpl<StandardMapper, StandardEntity> implements IStandardService {

	@Override
	public IPage<StandardVO> selectStandardPage(IPage<StandardVO> page, StandardVO standard) {
		return page.setRecords(baseMapper.selectStandardPage(page, standard));
	}


	@Override
	public List<StandardExcel> exportStandard(Wrapper<StandardEntity> queryWrapper) {
		List<StandardExcel> standardList = baseMapper.exportStandard(queryWrapper);
		//standardList.forEach(standard -> {
		//	standard.setTypeName(DictCache.getValue(DictEnum.YES_NO, Standard.getType()));
		//});
		return standardList;
	}

	@Override
	public List<StandardVO> lazyList(Long parentId, Map<String, Object> param) {
		if (Func.isEmpty(Func.toStr(param.get("parentId")))) {
			parentId = new Long(0);
		}
		if (Func.isNotEmpty(param.get("id"))) {
			parentId = null;
		}
		return baseMapper.lazyList(parentId, param);
	}


	@Override
	public Integer lazyListCount(Long parentId, Map<String, Object> param) {
		if (Func.isEmpty(Func.toStr(param.get("parentId")))) {
			parentId = null;
		}
		return baseMapper.lazyListCount(parentId, param);
	}

	@Override
	public Boolean submit(StandardEntity standard) {
		standard.setUploadUser(AuthUtil.getUserAccount());
		standard.setUploadTime(new Date());
		if (standard.getParentId() == null || standard.getParentId() == 0) {
			standard.setLevel(1);
		} else {
			if (Func.toLong(standard.getParentId()) == Func.toLong(standard.getId())) {
				throw new ServiceException("父节点不可选择自身!");
			}
			StandardEntity parentStandard = this.getById(standard.getParentId());
			if (parentStandard == null) {
				throw new ServiceException("父节点不存在或已被删除!");
			}
			standard.setLevel(parentStandard.getLevel() + 1);
		}
		this.saveOrUpdate(standard);
		return true;
	}

	@Override
	public List<StandardNodeVO> tree() {
		return ForestNodeMerger.merge(baseMapper.tree());
	}


}
