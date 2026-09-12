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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.StrategyConfig;
import com.baomidou.mybatisplus.generator.config.builder.ConfigBuilder;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import lombok.RequiredArgsConstructor;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.tool.utils.StringPool;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.develop.entity.Datasource;
import org.springblade.modules.develop.entity.ModelPrototype;
import org.springblade.modules.develop.mapper.ModelPrototypeMapper;
import org.springblade.modules.develop.service.IDatasourceService;
import org.springblade.modules.develop.service.IModelPrototypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据原型表 服务实现类
 *
 * @author Chill
 */
@Service
@RequiredArgsConstructor
public class ModelPrototypeServiceImpl extends BaseServiceImpl<ModelPrototypeMapper, ModelPrototype> implements IModelPrototypeService {

	private final IDatasourceService datasourceService;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitList(List<ModelPrototype> modelPrototypes) {
		modelPrototypes.forEach(modelPrototype -> {
			if (modelPrototype.getId() == null) {
				this.save(modelPrototype);
			} else {
				this.updateById(modelPrototype);
			}
		});
		return true;
	}

	@Override
	public List<ModelPrototype> prototypeList(Long modelId) {
		return this.list(Wrappers.<ModelPrototype>lambdaQuery().eq(ModelPrototype::getModelId, modelId));
	}

	@Override
	public TableInfo getTableInfo(String tableName, Long datasourceId) {
		Datasource datasource = datasourceService.getById(datasourceId);
		ConfigBuilder config = getConfigBuilder(datasource, tableName);
		List<TableInfo> tableInfoList = config.getTableInfoList();
		TableInfo tableInfo = null;
		Iterator<TableInfo> iterator = tableInfoList.stream().filter(table -> table.getName().equals(tableName)).collect(Collectors.toList()).iterator();
		if (iterator.hasNext()) {
			tableInfo = iterator.next();
			if (tableName.contains(StringPool.UNDERSCORE)) {
				tableInfo.setEntityName(tableInfo.getEntityName().replace(StringUtil.firstCharToUpper(tableName.split(StringPool.UNDERSCORE)[0]), StringPool.EMPTY));
			} else {
				tableInfo.setEntityName(StringUtil.firstCharToUpper(tableName));
			}
		}
		return tableInfo;
	}

	@Override
	public ConfigBuilder getConfigBuilder(Datasource datasource, String tableName) {
		StrategyConfig.Builder builder = new StrategyConfig.Builder();
		if (StringUtil.isNotBlank(tableName)) {
			builder.addInclude(tableName);
		}
		StrategyConfig strategyConfig = builder.entityBuilder()
			.naming(NamingStrategy.underline_to_camel)
			.columnNaming(NamingStrategy.underline_to_camel).build();
		DataSourceConfig datasourceConfig = new DataSourceConfig.Builder(
			datasource.getUrl(), datasource.getUsername(), datasource.getPassword()
		).build();
		return new ConfigBuilder(null, datasourceConfig, strategyConfig, null, null, null);
	}

}
