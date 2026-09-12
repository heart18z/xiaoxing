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
package org.springblade.modules.develop.service.impl;


import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springblade.modules.develop.entity.ThirdPartyTableColumnEntity;
import org.springblade.modules.develop.excel.ThirdPartyTableColumnExcel;
import org.springblade.modules.develop.mapper.ThirdPartyTableColumnMapper;
import org.springblade.modules.develop.vo.ThirdPartyTableColumnVO;
import org.springblade.modules.develop.service.IThirdPartyTableColumnService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseServiceImpl;
import java.util.List;
/**
 * 数据中台列信息 服务实现类
 *
 * @author BladeX
 * @since 2025-02-09
 */
@Service
public class ThirdPartyTableColumnServiceImpl extends BaseServiceImpl<ThirdPartyTableColumnMapper, ThirdPartyTableColumnEntity> implements IThirdPartyTableColumnService {


	@Override
	public IPage<ThirdPartyTableColumnVO> selectThirdPartyTableColumnPage(IPage<ThirdPartyTableColumnVO> page, ThirdPartyTableColumnVO thirdPartyTableColumn) {
		return page.setRecords(baseMapper.selectThirdPartyTableColumnPage(page, thirdPartyTableColumn));
	}


	@Override
	public List<ThirdPartyTableColumnExcel> exportThirdPartyTableColumn(Wrapper<ThirdPartyTableColumnEntity> queryWrapper) {
		List<ThirdPartyTableColumnExcel> thirdPartyTableColumnList = baseMapper.exportThirdPartyTableColumn(queryWrapper);
		//thirdPartyTableColumnList.forEach(thirdPartyTableColumn -> {
		//	thirdPartyTableColumn.setTypeName(DictCache.getValue(DictEnum.YES_NO, ThirdPartyTableColumn.getType()));
		//});
		return thirdPartyTableColumnList;
	}



}
