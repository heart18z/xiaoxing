package org.springblade.modules.standard.service.impl;

import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.hutool.db.ds.simple.SimpleDataSource;
import lombok.AllArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springblade.common.utils.DatasourceUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.develop.entity.Datasource;
import org.springblade.modules.develop.mapper.DatasourceMapper;
import org.springblade.modules.standard.service.IConfigDatabaseDataCloneService;
import org.springblade.modules.standard.vo.ConfigDatabaseDataClonePageVO;
import org.springblade.modules.standard.vo.DatabaseCloneVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ConfigDatabaseDataCloneServiceImpl implements IConfigDatabaseDataCloneService {

	private final DatasourceMapper datasourceMapper;



	@Override
	public ConfigDatabaseDataClonePageVO page(Long datasourceId,String tableName, Query query) throws SQLException {

		ConfigDatabaseDataClonePageVO res = new ConfigDatabaseDataClonePageVO();

		if (Func.isEmpty(tableName)){
			throw new ServiceException("请选择数据表进行查询");
		}
		// 获取数据源
		Datasource datasource = datasourceMapper.selectById(datasourceId);
		if (Func.isEmpty(datasource)){
			throw new ServiceException("未找到数据源");
		}


		//Long datasourceId = datasource.getId();

		// 获取字段集合
		List<Kv> fieldList = DatasourceUtil.getTableInfo(tableName, datasourceId)
			.stream()
			.map(field -> Kv.create()
				.set("label", field.getComment())
				.set("prop", field.getName().toLowerCase())
				.set("overHidden", true)
			).collect(Collectors.toList());

		// 生成数据源
		SimpleDataSource simpleDataSource = new SimpleDataSource(datasource.getUrl(),
			datasource.getUsername(),
			datasource.getPassword(),
			datasource.getDriverClass()
		);
		long total = Db.use(simpleDataSource)
			.count(Entity.create(tableName));

		PageResult<Entity> page = Db.use(simpleDataSource)
			.page(
				Entity.create(tableName),
				new Page(query.getCurrent() - 1, query.getSize())
			);

		res.setFields(fieldList);
		res.setRecords(page);
		res.setTotal(total);

		return res;

	}

	@Override
	@Transactional
	public boolean doSync(DatabaseCloneVO databaseCloneVO) throws SQLException {


		if (Func.isEmpty(databaseCloneVO.getSourceDataSourceId())
			||Func.isEmpty(databaseCloneVO.getTargetDataSourceId())
			||Func.isEmpty(databaseCloneVO.getTargetTableName())
			||Func.isEmpty(databaseCloneVO.getSourceTableName()))
		{
			throw new ServiceException("完善数据后进行同步！");
		}
		ConfigDatabaseDataClonePageVO sourceData = getSourceData(databaseCloneVO.getSourceDataSourceId(),databaseCloneVO.getSourceTableName());

		insertData(sourceData, databaseCloneVO.getTargetDataSourceId(),databaseCloneVO.getTargetTableName());

		return true;
	}

	private ConfigDatabaseDataClonePageVO getSourceData(Long datasourceId,String tableName) throws SQLException {
		ConfigDatabaseDataClonePageVO res = new ConfigDatabaseDataClonePageVO();

		// 获取数据源
		Datasource datasource = datasourceMapper.selectById(datasourceId);
		if (Func.isEmpty(datasource)){
			throw new ServiceException("未找到数据源");
		}

		// 获取字段集合
		List<String> fieldList = DatasourceUtil.getTableInfo(tableName, datasourceId)
			.stream().map(item->item.getName().toLowerCase()).collect(Collectors.toList());
		if (Func.isEmpty(fieldList)) {
			throw new ServiceException("获取表结构失败！");
		}
		// 生成数据源
		SimpleDataSource simpleDataSource = new SimpleDataSource(datasource.getUrl(),
			datasource.getUsername(),
			datasource.getPassword(),
			datasource.getDriverClass()
		);
		PageResult<Entity> page = Db.use(simpleDataSource)
			.page(
				Entity.create(tableName),
				new Page(0, 50000)
			);

		res.setProps(fieldList);
		res.setRecords(page);
		return res;
	}


	private void insertData(ConfigDatabaseDataClonePageVO data,Long datasourceId,String tableName) throws SQLException {
		// 获取数据源
		Datasource datasource = datasourceMapper.selectById(datasourceId);
		if (Func.isEmpty(datasource)){
			throw new ServiceException("未找到数据源");
		}

		// 获取字段集合
		List<String> targetFieldList = DatasourceUtil.getTableInfo(tableName, datasourceId)
			.stream().map(item->item.getName().toLowerCase()).collect(Collectors.toList());

		// 生成数据源
		SimpleDataSource simpleDataSource = new SimpleDataSource(datasource.getUrl(),
			datasource.getUsername(),
			datasource.getPassword(),
			datasource.getDriverClass()
		);
		if (Func.isEmpty(targetFieldList)) {
			throw new ServiceException("获取表结构失败！");
		}
		boolean check= CollectionUtils.isEqualCollection(data.getProps(), targetFieldList);
		if (!check) {
			throw new ServiceException("数据表结构不相同，请选择相同表结构的表进行同步！");
//			if (Func.isEmpty(targetFieldList)) {
//
//			}
		}
		Db.use(simpleDataSource).del(Entity.create(tableName).set("id","!= null"));
		int[] i = Db.use(simpleDataSource).insert(data.getRecords());
//		return t;
	}

}
