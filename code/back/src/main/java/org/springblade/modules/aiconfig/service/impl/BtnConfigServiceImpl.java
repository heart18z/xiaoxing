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
package org.springblade.modules.aiconfig.service.impl;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.aiconfig.entity.BtnConfigDetailEntity;
import org.springblade.modules.aiconfig.entity.BtnConfigEntity;
import org.springblade.modules.aiconfig.excel.BtnConfigExcel;
import org.springblade.modules.aiconfig.mapper.BtnConfigMapper;
import org.springblade.modules.aiconfig.service.IBtnConfigDetailService;
import org.springblade.modules.aiconfig.service.IBtnConfigService;
import org.springblade.modules.aiconfig.vo.BtnConfigVO;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI按钮接口配置表 服务实现类
 *
 * @author wxd
 * @since 2026-03-25
 */
@Service
public class BtnConfigServiceImpl extends BaseServiceImpl<BtnConfigMapper, BtnConfigEntity> implements IBtnConfigService {

	private final IBtnConfigDetailService btnConfigDetailService;

	public BtnConfigServiceImpl(@Lazy IBtnConfigDetailService btnConfigDetailService) {
		this.btnConfigDetailService = btnConfigDetailService;
	}

	@Override
	public BtnConfigEntity detail(BtnConfigEntity btnConfig) {
		BtnConfigEntity btnConfigEntity = getOne(Condition.getQueryWrapper(btnConfig));
		if (Func.isNotEmpty(btnConfigEntity)) {
			Long configId = btnConfigEntity.getId();
			List<BtnConfigDetailEntity> detailList = btnConfigDetailService.list(Wrappers.<BtnConfigDetailEntity>lambdaQuery()
				.eq(BtnConfigDetailEntity::getBtnConfigId, configId)
				.eq(BtnConfigDetailEntity::getIsDeleted, 0));
			if (Func.isNotEmpty(detailList)) {
				btnConfigEntity.setDetailList(detailList);
			}
		}
		return btnConfigEntity;
	}

	@Override
	public IPage<BtnConfigVO> selectBtnConfigPage(IPage<BtnConfigVO> page, BtnConfigVO btnConfig) {
		return page.setRecords(baseMapper.selectBtnConfigPage(page, btnConfig));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submit(BtnConfigEntity btnConfig) {

		// 先保存主表，确保能拿到 id（新增场景）
		boolean flag = this.saveOrUpdate(btnConfig);
		if (!flag) {
			throw new ServiceException("新增AI按钮配置失败");
		}

		// 修改/提交时：清理旧明细（只清理未逻辑删除的），避免“旧数据残留/回显错乱”
		if (Func.isNotEmpty(btnConfig.getId())) {
			List<BtnConfigDetailEntity> existed = btnConfigDetailService.list(
				Wrappers.<BtnConfigDetailEntity>lambdaQuery()
					.eq(BtnConfigDetailEntity::getBtnConfigId, btnConfig.getId())
					.eq(BtnConfigDetailEntity::getIsDeleted, 0)
			);
			if (Func.isNotEmpty(existed)) {
				List<Long> deletedIds = existed.stream().map(BtnConfigDetailEntity::getId).collect(Collectors.toList());
				// 采用逻辑删除与查询条件一致，避免“查到已删数据”的错乱
				btnConfigDetailService.deleteLogic(deletedIds);
			}
		}

		// 重新插入新明细（全量覆盖）
		if (Func.isNotEmpty(btnConfig.getDetailList())) {
			List<BtnConfigDetailEntity> detailEntityList = btnConfig.getDetailList().stream()
				.filter(Func::isNotEmpty)
				.collect(Collectors.toList());
			if (Func.isNotEmpty(detailEntityList)) {
				detailEntityList.forEach(detail -> {
					// 强制走新增，避免前端携带 id 导致“更新到别的行/顺序错乱”
					detail.setId(null);
					detail.setBtnConfigId(btnConfig.getId());
				});
				flag = btnConfigDetailService.saveBatch(detailEntityList);
				if (!flag) {
					throw new ServiceException("新增背景资料列表失败");
				}
			}
		}

		return true;
	}


	@Override
	public List<BtnConfigExcel> exportBtnConfig(Wrapper<BtnConfigEntity> queryWrapper) {
		List<BtnConfigExcel> btnConfigList = baseMapper.exportBtnConfig(queryWrapper);
		//btnConfigList.forEach(btnConfig -> {
		//	btnConfig.setTypeName(DictCache.getValue(DictEnum.YES_NO, BtnConfig.getType()));
		//});
		return btnConfigList;
	}

}
