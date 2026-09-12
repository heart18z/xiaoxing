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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.modules.standard.entity.BizMenuTableEntity;
import org.springblade.modules.standard.excel.BizMenuTableExcel;
import org.springblade.modules.standard.mapper.BizMenuTableMapper;
import org.springblade.modules.standard.service.IBizMenuTableService;
import org.springblade.modules.standard.vo.BizMenuTableVO;
import org.springblade.modules.system.entity.DictBiz;
import org.springblade.modules.system.service.IDictBizService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 业务库表 服务实现类
 *
 * @author BladeX
 * @since 2023-07-21
 */
@Service
@AllArgsConstructor
public class BizMenuTableServiceImpl extends BaseServiceImpl<BizMenuTableMapper, BizMenuTableEntity> implements IBizMenuTableService {

	private final IDictBizService dictBizService;

	@Override
	public IPage<BizMenuTableVO> selectBizMenuTablePage(IPage<BizMenuTableVO> page, BizMenuTableVO bizMenuTable) {
		return page.setRecords(baseMapper.selectBizMenuTablePage(page, bizMenuTable));
	}


	@Override
	public List<BizMenuTableExcel> exportBizMenuTable(Wrapper<BizMenuTableEntity> queryWrapper) {
		List<BizMenuTableExcel> bizMenuTableList = baseMapper.exportBizMenuTable(queryWrapper);
		//bizMenuTableList.forEach(bizMenuTable -> {
		//	bizMenuTable.setTypeName(DictCache.getValue(DictEnum.YES_NO, BizMenuTable.getType()));
		//});
		return bizMenuTableList;
	}

	@Override
	public IPage<BizMenuTableEntity> getPageByUseType(IPage<BizMenuTableEntity> page, QueryWrapper<BizMenuTableEntity> queryWrapper) {

		List<DictBiz> list = dictBizService.getList("table_use_type");
		List<String> values = list.stream().map(DictBiz::getDictKey).collect(Collectors.toList());
		if (values.size()>0) {
			queryWrapper = queryWrapper.in("table_type",values);
		}

		IPage<BizMenuTableEntity> pages = this.page(page, queryWrapper);
		return pages;
	}


}
