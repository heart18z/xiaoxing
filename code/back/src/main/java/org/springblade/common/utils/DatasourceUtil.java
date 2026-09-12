package org.springblade.common.utils;

import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.StrategyConfig;
import com.baomidou.mybatisplus.generator.config.builder.ConfigBuilder;
import com.baomidou.mybatisplus.generator.config.po.TableField;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.SpringUtil;
import org.springblade.core.tool.utils.StringPool;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.develop.entity.Datasource;
import org.springblade.modules.develop.service.IDatasourceService;
import org.springblade.modules.standard.support.mpgenerator.TableFieldQuery;
import org.springblade.modules.standard.support.mpgenerator.TableQuery;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 为解决调用慢的问题，已将其迁移到DatasourceCache中使用缓存调用
 */
public class DatasourceUtil {

	private static final IDatasourceService datasourceService;

	static {
		datasourceService = SpringUtil.getBean(IDatasourceService.class);
	}

	/**
	 * 获取表信息
	 *
	 * @param tableName    表名
	 * @param datasourceId 数据源主键
	 */
	public static List<TableField> getTableInfo(String tableName, Long datasourceId) {
		ConfigBuilder config = getConfigBuilder(datasourceId, tableName);
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
		return tableInfo.getFields();
	}

	/**
	 * 获取表配置信息
	 *
	 * @param datasourceId 数据源信息
	 */
	public static ConfigBuilder getConfigBuilder(Long datasourceId) {
		return getConfigBuilder(datasourceId, null);
	}

	/**
	 * 获取表配置信息
	 *
	 * @param datasourceId 数据源信息
	 * @param tableName    表名
	 */
	public static ConfigBuilder getConfigBuilder(Long datasourceId, String tableName) {
		Datasource datasource = datasourceService.getById(datasourceId);
		if (Func.isEmpty(datasource)) {
			throw new ServiceException("源数据库不存在");
		}
		StrategyConfig.Builder builder = new StrategyConfig.Builder();
		if (StringUtil.isNotBlank(tableName)) {
			builder.addInclude(tableName);
		}
		StrategyConfig strategyConfig = builder.entityBuilder()
			.naming(NamingStrategy.underline_to_camel)
			.columnNaming(NamingStrategy.underline_to_camel).build();

		DataSourceConfig.Builder dataSourceConfigBuilder = new DataSourceConfig.Builder(
			datasource.getUrl(), datasource.getUsername(), datasource.getPassword()
		).addConnectionProperty("remarks", "true")
			.addConnectionProperty("useInformationSchema", "true");

		if (Func.isNotEmpty(tableName)) {
			dataSourceConfigBuilder.databaseQueryClass(TableFieldQuery.class);
		} else {
			dataSourceConfigBuilder.databaseQueryClass(TableQuery.class);
		}


		return new ConfigBuilder(null, dataSourceConfigBuilder.build(), strategyConfig, null, null, null);
	}
}
