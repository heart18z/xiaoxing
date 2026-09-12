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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.develop.entity.Datasource;
import org.springblade.modules.develop.mapper.DatasourceMapper;
import org.springblade.modules.develop.service.IDatasourceService;
import org.springblade.modules.develop.vo.TableInfoVO;
import org.springblade.modules.standard.entity.BizMenuTableEntity;
import org.springblade.modules.standard.service.IBizMenuTableService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据源配置表 服务实现类
 *
 * @author Chill
 */
@Service
@AllArgsConstructor
public class DatasourceServiceImpl extends BaseServiceImpl<DatasourceMapper, Datasource> implements IDatasourceService {

	private final IBizMenuTableService bizMenuTableService;
	@Override
	public List<Datasource> listByMenuTable() {
		List<Long> menuTableSourceId = bizMenuTableService.list(new LambdaQueryWrapper<BizMenuTableEntity>()
			.select(BizMenuTableEntity::getDatasourceId)
			.groupBy(BizMenuTableEntity::getDatasourceId)).stream().map(BizMenuTableEntity::getDatasourceId).collect(Collectors.toList());
		if (menuTableSourceId.size()<1) {
			return new ArrayList<>();
		}
		return this.list(new LambdaQueryWrapper<Datasource>().in(Datasource::getId,menuTableSourceId));
	}

	@Override
	public List<TableInfoVO> getTableListByMenuTable(Long datasourceId) {
		if (Func.isEmpty(datasourceId)) {
			return new ArrayList<>();
		}
		List<TableInfoVO> menuTableName = bizMenuTableService.list(new LambdaQueryWrapper<BizMenuTableEntity>()
			.eq(BizMenuTableEntity::getDatasourceId,datasourceId)).stream()
			.filter(item->Func.isNotBlank(item.getTableName()))
			.map(item->{
				String name = item.getTableName().split(":")[0];
				TableInfoVO tableInfo = new TableInfoVO();
				tableInfo.setName(name);
				tableInfo.setComment(item.getTableName());
				return tableInfo;
			})
			.collect(Collectors.toList());
		return menuTableName;
	}

	@Override
	public boolean submit(Datasource datasource) {
		datasource.setUrl(getDatasourceUrl(datasource));

		return this.saveOrUpdate(datasource);
	}

	private static String getDatasourceUrl(Datasource datasource){
		String type = datasource.getDriverClass();

		if (Func.isBlank(type)) {
			return null;
		}
		switch (type){
			case "com.mysql.cj.jdbc.Driver":
				return "jdbc:mysql://"+datasource.getHost()+":"+
					datasource.getPort()+"/"+
					datasource.getDatabaseName()+
					"?useSSL=false&useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true&serverTimezone=GMT%2B8&nullCatalogMeansCurrent=true&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true";
			case "org.postgresql.Driver":
				return "jdbc:postgresql://"+datasource.getHost()+":"+
				datasource.getPort()+"/"+
				datasource.getDatabaseName();
			case "oracle.jdbc.OracleDriver":
				return "jdbc:oracle:thin:@"+datasource.getHost()+":"+
					datasource.getPort()+":"+
					datasource.getDatabaseName();
			case "com.microsoft.sqlserver.jdbc.SQLServerDriver":
				return "jdbc:sqlserver://"+datasource.getHost()+":"+
					datasource.getPort()+";DatabaseName="+
					datasource.getDatabaseName();
			case "dm.jdbc.driver.DmDriver":
				return "jdbc:dm://"+datasource.getHost()+":"+
					datasource.getPort()+"/"+
					datasource.getDatabaseName()+"?zeroDateTimeBehavior=convertToNull&useUnicode=true&characterEncoding=utf-8";

		}
		return "";

	}
}
