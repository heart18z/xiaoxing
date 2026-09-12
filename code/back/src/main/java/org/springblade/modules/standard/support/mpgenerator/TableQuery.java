package org.springblade.modules.standard.support.mpgenerator;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.generator.config.builder.ConfigBuilder;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.jdbc.DatabaseMetaDataWrapper;
import com.baomidou.mybatisplus.generator.query.AbstractDatabaseQuery;
import com.baomidou.mybatisplus.generator.type.TypeRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TableQuery extends AbstractDatabaseQuery {

	private final TypeRegistry typeRegistry;

	protected final DatabaseMetaDataWrapper databaseMetaDataWrapper;

	public TableQuery(@NotNull ConfigBuilder configBuilder) {
		super(configBuilder);
		typeRegistry = new TypeRegistry(configBuilder.getGlobalConfig());
		this.databaseMetaDataWrapper = new DatabaseMetaDataWrapper(dataSourceConfig.getConn(), dataSourceConfig.getSchemaName());
	}

	@Override
	public @NotNull List<TableInfo> queryTables() {
		try {
			boolean isInclude = strategyConfig.getInclude().size() > 0;
			boolean isExclude = strategyConfig.getExclude().size() > 0;
			//所有的表信息
			List<TableInfo> tableList = new ArrayList<>();
			List<DatabaseMetaDataWrapper.Table> tables = this.getTables();
			//需要反向生成或排除的表信息
			List<TableInfo> includeTableList = new ArrayList<>();
			List<TableInfo> excludeTableList = new ArrayList<>();
			tables.forEach(table -> {
				String tableName = table.getName();
				if (StringUtils.isNotBlank(tableName)) {
					TableInfo tableInfo = new TableInfo(this.configBuilder, tableName);
					tableInfo.setComment(table.getRemarks());
					if (isInclude && strategyConfig.matchIncludeTable(tableName)) {
						includeTableList.add(tableInfo);
					} else if (isExclude && strategyConfig.matchExcludeTable(tableName)) {
						excludeTableList.add(tableInfo);
					}
					tableList.add(tableInfo);
				}
			});
			filter(tableList, includeTableList, excludeTableList);
			// 性能优化，只处理需执行表字段 https://github.com/baomidou/mybatis-plus/issues/219
			return tableList;
		} finally {
			// 数据库操作完成,释放连接对象
			databaseMetaDataWrapper.closeConnection();
		}
	}

	protected List<DatabaseMetaDataWrapper.Table> getTables() {
		// 是否跳过视图
		boolean skipView = strategyConfig.isSkipView();
		// 获取表过滤
		String tableNamePattern = null;
		if (strategyConfig.getLikeTable() != null) {
			tableNamePattern = strategyConfig.getLikeTable().getValue();
		}
		return databaseMetaDataWrapper.getTables(tableNamePattern, skipView ? new String[]{"TABLE"} : new String[]{"TABLE", "VIEW"});
	}
}
