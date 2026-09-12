package org.springblade.modules.standard.support.mpgenerator;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.generator.config.builder.ConfigBuilder;
import com.baomidou.mybatisplus.generator.config.builder.Entity;
import com.baomidou.mybatisplus.generator.config.po.TableField;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.config.rules.IColumnType;
import com.baomidou.mybatisplus.generator.jdbc.DatabaseMetaDataWrapper;
import com.baomidou.mybatisplus.generator.query.AbstractDatabaseQuery;
import com.baomidou.mybatisplus.generator.type.ITypeConvertHandler;
import com.baomidou.mybatisplus.generator.type.TypeRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TableFieldQuery extends AbstractDatabaseQuery {

	private final TypeRegistry typeRegistry;

	protected final DatabaseMetaDataWrapper databaseMetaDataWrapper;

	public TableFieldQuery(@NotNull ConfigBuilder configBuilder) {
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
			tableList.forEach(this::convertTableFields);
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

	protected void convertTableFields(@NotNull TableInfo tableInfo) {
		String tableName = tableInfo.getName();
		Map<String, DatabaseMetaDataWrapper.Column> columnsInfoMap = getColumnsInfo(tableName);
		Entity entity = strategyConfig.entity();
		columnsInfoMap.forEach((k, columnInfo) -> {
			String columnName = columnInfo.getName();
			TableField field = new TableField(this.configBuilder, columnName);
			// 处理ID
			if (columnInfo.isPrimaryKey()) {
				field.primaryKey(columnInfo.isAutoIncrement());
				tableInfo.setHavePrimaryKey(true);
				if (field.isKeyIdentityFlag() && entity.getIdType() != null) {
					LOGGER.warn("当前表[{}]的主键为自增主键，会导致全局主键的ID类型设置失效!", tableName);
				}
			}
			field.setColumnName(columnName).setComment(columnInfo.getRemarks());
			String propertyName = entity.getNameConvert().propertyNameConvert(field);
			// 设置字段的元数据信息
			TableField.MetaInfo metaInfo = new TableField.MetaInfo(columnInfo, tableInfo);
			IColumnType columnType = typeRegistry.getColumnType(metaInfo);
			ITypeConvertHandler typeConvertHandler = dataSourceConfig.getTypeConvertHandler();
			if (typeConvertHandler != null) {
				columnType = typeConvertHandler.convert(globalConfig, typeRegistry, metaInfo);
			}
			field.setPropertyName(propertyName, columnType);
			field.setMetaInfo(metaInfo);
			tableInfo.addField(field);
		});
		tableInfo.processTable();
	}

	protected Map<String, DatabaseMetaDataWrapper.Column> getColumnsInfo(String tableName) {
		return databaseMetaDataWrapper.getColumnsInfo(tableName, true);
	}
}
