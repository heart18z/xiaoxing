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
import org.springblade.core.log.exception.ServiceException;
import org.springblade.modules.system.entity.PropertyRegionEntity;
import org.springblade.modules.system.vo.PropertyRegionVO;
import org.springblade.modules.system.excel.PropertyRegionExcel;
import org.springblade.modules.system.mapper.PropertyRegionMapper;
import org.springblade.modules.system.service.IPropertyRegionService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 * 物业信息表 服务实现类
 *
 * @author BladeX
 * @since 2024-08-14
 */
@Service
public class PropertyRegionServiceImpl extends BaseServiceImpl<PropertyRegionMapper, PropertyRegionEntity> implements IPropertyRegionService {


	@Override
	public IPage<PropertyRegionVO> selectPropertyRegionPage(IPage<PropertyRegionVO> page, PropertyRegionVO propertyRegion) {
		return page.setRecords(baseMapper.selectPropertyRegionPage(page, propertyRegion));
	}


	@Override
	public List<PropertyRegionExcel> exportPropertyRegion(Wrapper<PropertyRegionEntity> queryWrapper) {
		List<PropertyRegionExcel> propertyRegionList = baseMapper.exportPropertyRegion(queryWrapper);
		//propertyRegionList.forEach(propertyRegion -> {
		//	propertyRegion.setTypeName(DictCache.getValue(DictEnum.YES_NO, PropertyRegion.getType()));
		//});
		return propertyRegionList;
	}

	@Override
	@Transactional
	public boolean submit(PropertyRegionEntity propertyRegion) {
		PropertyRegionEntity propertyRegionEntity = this.getOne(new LambdaQueryWrapper<PropertyRegionEntity>()
			.ne(Func.notNull(propertyRegion.getId()),PropertyRegionEntity::getId,propertyRegion.getId())
			.eq(PropertyRegionEntity::getPropertyCode,propertyRegion.getPropertyCode())
		);

		if (propertyRegionEntity !=null) {
			throw new ServiceException("物业编号不能重复！");
		}

		return this.saveOrUpdate(propertyRegion);
	}

}
