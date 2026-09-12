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


import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.ds.simple.SimpleDataSource;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.AllArgsConstructor;
import org.springblade.common.constant.CommonConstant;
import org.springblade.common.utils.CommonUtil;
import org.springblade.common.utils.SqlTableGenerator;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.develop.entity.Datasource;
import org.springblade.modules.develop.entity.ThirdPartyTableColumnEntity;
import org.springblade.modules.develop.entity.ThirdPartyTableEntity;
import org.springblade.modules.develop.service.IDatasourceService;
import org.springblade.modules.develop.service.IThirdPartyTableColumnService;
import org.springblade.modules.develop.vo.ThirdPartyTableColumnVO;
import org.springblade.modules.develop.vo.ThirdPartyTableVO;
import org.springblade.modules.develop.excel.ThirdPartyTableExcel;
import org.springblade.modules.develop.mapper.ThirdPartyTableMapper;
import org.springblade.modules.develop.service.IThirdPartyTableService;
import org.springblade.modules.develop.wrapper.ThirdPartyTableColumnWrapper;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 中台数据 服务实现类
 *
 * @author BladeX
 * @since 2025-02-09
 */
@Service
@AllArgsConstructor
public class ThirdPartyTableServiceImpl extends BaseServiceImpl<ThirdPartyTableMapper, ThirdPartyTableEntity> implements IThirdPartyTableService {
	private final IThirdPartyTableColumnService thirdPartyTableColumnService;
	private final IDatasourceService datasourceService;

	private final Integer IS_DELETED = BladeConstant.DB_IS_DELETED;

	private final Integer IS_STATUS_NORMAL = CommonConstant.DB_STATUS_NORMAL;

	@Override
	public IPage<ThirdPartyTableVO> selectThirdPartyTablePage(IPage<ThirdPartyTableVO> page, ThirdPartyTableVO thirdPartyTable) {
		return page.setRecords(baseMapper.selectThirdPartyTablePage(page, thirdPartyTable));
	}


	@Override
	public List<ThirdPartyTableExcel> exportThirdPartyTable(Wrapper<ThirdPartyTableEntity> queryWrapper) {
		List<ThirdPartyTableExcel> thirdPartyTableList = baseMapper.exportThirdPartyTable(queryWrapper);
		//thirdPartyTableList.forEach(thirdPartyTable -> {
		//	thirdPartyTable.setTypeName(DictCache.getValue(DictEnum.YES_NO, ThirdPartyTable.getType()));
		//});
		return thirdPartyTableList;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submit(ThirdPartyTableVO thirdPartyTableVO) {
		if (thirdPartyTableVO.getId() ==null) {
			return this.saveTableData(thirdPartyTableVO);
		} else {
			return this.updateTableData(thirdPartyTableVO);
		}

	}

	@Override
	public ThirdPartyTableVO detail(Long id) {
		ThirdPartyTableEntity entity = this.getById(id);
		if (entity!=null) {
			ThirdPartyTableVO vo = BeanUtil.copy(entity,ThirdPartyTableVO.class);
			List<ThirdPartyTableColumnEntity> entities = thirdPartyTableColumnService.list(
				new LambdaQueryWrapper<ThirdPartyTableColumnEntity>()
					.eq(ThirdPartyTableColumnEntity::getTableId,entity.getId())
					.orderByAsc(ThirdPartyTableColumnEntity::getColumnSort)
			);
			List<ThirdPartyTableColumnVO> voList =  ThirdPartyTableColumnWrapper.build().listVO(entities);

			if (IS_STATUS_NORMAL.equals(entity.getStatus())) {
				Datasource datasource = datasourceService.getById(entity.getDatabaseId());
				if (datasource==null) {
					throw new ServiceException("未找到数据源");
				}
				SimpleDataSource simpleDataSource = new SimpleDataSource(datasource.getUrl(),
					datasource.getUsername(),
					datasource.getPassword(),
					datasource.getDriverClass()
				);
				try {
					Number allCount= Db.use(simpleDataSource).count("select * from `"+entity.getTableName()+"` limit 1");
					if (allCount.intValue()<1) {
						vo.setIsExistData("2");
						voList = voList.stream().peek(i->i.setIsExistData("2")).collect(Collectors.toList());
					}else {
						vo.setIsExistData("1");
//						List<String> columnNames = voList.stream().map(ThirdPartyTableColumnVO::getColumnName).collect(Collectors.toList());
						for(ThirdPartyTableColumnVO i : voList) {
							Number columnCount= Db.use(simpleDataSource).count("select * from `"+entity.getTableName()+"` where `"+i.getColumnName()+"` is not null limit 1");
							if (columnCount.intValue()<1) {
								i.setIsExistData("2");
							}else {
								i.setIsExistData("1");
							}
						}
					}


				} catch (SQLException e) {
					throw new ServiceException("获取表字段失败："+e.getMessage());
				}
			} else {
				voList = voList.stream().peek(i->i.setIsExistData("2")).collect(Collectors.toList());
				vo.setIsExistData("2");
			}


			vo.setColumnVoList(voList);
			return vo;
		}
		return null;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean del(List<Long> longList) {
		List<ThirdPartyTableEntity> entities = this.listByIds(longList);
		if (entities.isEmpty()) {
			throw new ServiceException("未找到对应数据请刷新后重试");
		}
		List<SimpleDataSource> dataSources = new ArrayList<>();
		for (ThirdPartyTableEntity entity : entities) {

			if (!IS_STATUS_NORMAL.equals(entity.getStatus())) {
				continue;
			}
			Datasource datasource = datasourceService.getById(entity.getDatabaseId());
			if (datasource==null) {
				throw new ServiceException("未找到数据源");
			}
			SimpleDataSource simpleDataSource = new SimpleDataSource(datasource.getUrl(),
				datasource.getUsername(),
				datasource.getPassword(),
				datasource.getDriverClass()
			);
			long allCount = 0;
			try {
				 allCount= Db.use(simpleDataSource)
					.count("select * from `"+entity.getTableName()+"` limit 1");
			} catch (Exception e) {
				throw new ServiceException("删除表失败："+e.getMessage());
			}
			if (allCount>0) {
				throw new ServiceException("表中存在数据，请先清空数据再删除！");
			}
			dataSources.add(simpleDataSource);
		}



		this.deleteLogic(longList);
		dataSources.forEach(i->{
			try {
				Db.use(i).execute("drop table IF EXISTS `"+entities.get(dataSources.indexOf(i)).getTableName()+"`");
			} catch (SQLException e) {
				throw new ServiceException("删除表失败："+e.getMessage());
			}
		});

		return true;
	}

	private boolean saveTableData(ThirdPartyTableVO vo) {
		Long id = IdWorker.getId();
		ThirdPartyTableEntity entity = BeanUtil.copy(vo,ThirdPartyTableEntity.class);
		entity.setId(id);
		List<ThirdPartyTableColumnEntity> columnList = vo.getColumnList();
		if (columnList==null || columnList.isEmpty()) {
			throw new ServiceException("请添加字段创建表结构！");
		}
		this.save(entity);
		int sort = 1;
		columnList =columnList.stream().filter(i-> !IS_DELETED.equals(i.getIsDeleted())).peek(i->{
			if (Func.isNull(i.getColumnInfoName())) {
				i.setColumnInfoName("");
			}
			if (Func.isNull(i.getColumnInfoDesc())) {
				i.setColumnInfoDesc("");
			}
		}).collect(Collectors.toList());
		for (ThirdPartyTableColumnEntity column : columnList) {
			column.setTableId(id);
			column.setColumnSort(sort);
			column.setColumnComment(column.getColumnInfoName()+"_"+column.getColumnInfoDesc());
			if (column.getColumnLong() ==null || column.getColumnLong() == 0) {
				column.setColumnLong(null);
			}
			if (column.getColumnDecimal() ==null || column.getColumnDecimal() == 0) {
				column.setColumnDecimal(null);
			}
			sort++;
		}
		if (columnList.isEmpty()) {
			throw new ServiceException("请添加字段创建表结构！");
		}
		thirdPartyTableColumnService.saveBatch(columnList);

		if (entity.getStatus() == 1) {
			this.createMysqlTable(id);
		}
		return true;
	}

	private boolean updateTableData(ThirdPartyTableVO vo) {
		ThirdPartyTableEntity entity = BeanUtil.copy(vo,ThirdPartyTableEntity.class);
		List<ThirdPartyTableColumnEntity> columnList = vo.getColumnList();
		if (columnList==null || columnList.isEmpty()) {
			throw new ServiceException("请添加字段创建表结构！");
		}

		List<ThirdPartyTableColumnEntity> delDate = new ArrayList<>();
		columnList = columnList.stream().peek(i->{
			if (Func.isNull(i.getColumnInfoName())) {
				i.setColumnInfoName("");
			}
			if (Func.isNull(i.getColumnInfoDesc())) {
				i.setColumnInfoDesc("");
			}
			if (IS_DELETED.equals(i.getIsDeleted())&& i.getId()!=null) {
				delDate.add(i);
			}
		}).filter(i->!IS_DELETED.equals(i.getIsDeleted())).collect(Collectors.toList());
		int sort = 1;
		for (ThirdPartyTableColumnEntity column : columnList) {
			column.setTableId(entity.getId());
			column.setColumnSort(sort);
			column.setColumnComment(column.getColumnInfoName()+"_"+column.getColumnInfoDesc());
			if (column.getColumnLong() ==null || column.getColumnLong() == 0) {
				column.setColumnLong(null);
			}
			if (column.getColumnDecimal() ==null || column.getColumnDecimal() == 0) {
				column.setColumnDecimal(null);
			}
			sort++;
		}
		if (columnList.isEmpty()) {
			throw new ServiceException("请添加字段创建表结构！");
		}
		if (entity.getStatus() == 1) {
			ThirdPartyTableEntity dbE =  this.getById(entity.getId());
			if (dbE.getStatus() != 1) {
				this.createMysqlTable(entity.getId());
			}else {
				updateMysqlTable(entity.getId(),delDate,columnList);
			}

		}
		this.updateById(entity);
		thirdPartyTableColumnService.saveOrUpdateBatch(columnList);
		if (!delDate.isEmpty()) {
			thirdPartyTableColumnService.removeByIds(delDate.stream()
				.map(ThirdPartyTableColumnEntity::getId)
				.collect(Collectors.toList()));
		}

		return true;
	}


	private void createMysqlTable(Long TableId) {
		ThirdPartyTableEntity entity = this.getById(TableId);
		if (entity==null) {
			throw new ServiceException("未找到表信息");
		}
		List<ThirdPartyTableColumnEntity> columnList = thirdPartyTableColumnService.list(
			new LambdaQueryWrapper<ThirdPartyTableColumnEntity>()
				.eq(ThirdPartyTableColumnEntity::getTableId,entity.getId())
				.orderByAsc(ThirdPartyTableColumnEntity::getColumnSort)
		);

		String sql = SqlTableGenerator.generateCreateTableSql(entity.getTableName(),entity.getTableComment(),columnList);
		Datasource datasource = datasourceService.getById(entity.getDatabaseId());
		if (datasource==null) {
			throw new ServiceException("未找到数据源");
		}
		SimpleDataSource simpleDataSource = new SimpleDataSource(datasource.getUrl(),
			datasource.getUsername(),
			datasource.getPassword(),
			datasource.getDriverClass()
		);
		try {
			Db.use(simpleDataSource).execute(sql);
		} catch (Exception e) {
			if (e.getMessage().contains("already exists")) {
				throw new ServiceException("表已存在，请勿重复创建！");
			}
			throw new ServiceException("创建表失败："+e.getMessage());
		}
	}


	private void updateMysqlTable(Long TableId, List<ThirdPartyTableColumnEntity> dropList,
								  List<ThirdPartyTableColumnEntity> saveOrUpdateList) {
		ThirdPartyTableEntity entity = this.getById(TableId);
		if (entity==null) {
			throw new ServiceException("未找到表信息");
		}

		List<ThirdPartyTableColumnEntity> oldColumnList = thirdPartyTableColumnService.list(
			new LambdaQueryWrapper<ThirdPartyTableColumnEntity>()
				.eq(ThirdPartyTableColumnEntity::getTableId,entity.getId())
				.orderByAsc(ThirdPartyTableColumnEntity::getColumnSort)
		);

		List<String> sql = SqlTableGenerator.generateAlterTableSQL(
			entity.getTableName(),
			entity.getTableComment(),
			dropList,
			saveOrUpdateList,
			oldColumnList
			);
		Datasource datasource = datasourceService.getById(entity.getDatabaseId());
		if (datasource==null) {
			throw new ServiceException("未找到数据源");
		}
		SimpleDataSource simpleDataSource = new SimpleDataSource(datasource.getUrl(),
			datasource.getUsername(),
			datasource.getPassword(),
			datasource.getDriverClass()
		);
		try {
			sql.forEach(i->{
                try {
                    Db.use(simpleDataSource).execute(i);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
		} catch (Exception e) {
			throw new ServiceException("修改表结构失败："+e.getMessage());
		}
	}

}
