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


import org.springblade.modules.system.entity.OptimizeEntity;
import org.springblade.modules.system.excel.OptimizeExcel;
import org.springblade.modules.system.mapper.OptimizeMapper;
import org.springblade.modules.system.vo.OptimizeVO;
import org.springblade.modules.system.service.IOptimizeService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseServiceImpl;
import java.util.List;
/**
 * 优化内容 服务实现类
 *
 * @author BladeX
 * @since 2024-08-04
 */
@Service
public class OptimizeServiceImpl extends BaseServiceImpl<OptimizeMapper, OptimizeEntity> implements IOptimizeService {


	@Override
	public IPage<OptimizeVO> selectOptimizePage(IPage<OptimizeVO> page, OptimizeVO optimize) {
		return page.setRecords(baseMapper.selectOptimizePage(page, optimize));
	}


	@Override
	public List<OptimizeExcel> exportOptimize(Wrapper<OptimizeEntity> queryWrapper) {
		List<OptimizeExcel> optimizeList = baseMapper.exportOptimize(queryWrapper);
		//optimizeList.forEach(optimize -> {
		//	optimize.setTypeName(DictCache.getValue(DictEnum.YES_NO, Optimize.getType()));
		//});
		return optimizeList;
	}

}
