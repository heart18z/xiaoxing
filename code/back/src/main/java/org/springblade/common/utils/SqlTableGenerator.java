package org.springblade.common.utils;

import org.apache.commons.collections.CollectionUtils;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.develop.entity.ThirdPartyTableColumnEntity;

import java.util.*;
import java.util.stream.Collectors;

public class SqlTableGenerator {
	/**
	 * 根据传入的参数生成表结构 SQL 语句
	 * @param tableName 表名称
	 * @param tableComment 表注释
	 * @param columns 列信息
	 * @return SQL 创建表语句
	 */
	public static String generateCreateTableSql(String tableName, String tableComment, List<ThirdPartyTableColumnEntity> columns) {
		StringBuilder sql = new StringBuilder();

		// 生成创建表的基本 SQL
		sql.append("CREATE TABLE `").append(tableName).append("` (\n");
		List<String> primaryKeyList = new ArrayList<>();
		// 迭代列信息，构建列定义
		for (int i = 0; i < columns.size(); i++) {
			ThirdPartyTableColumnEntity column = columns.get(i);
			sql.append(buildColumnByEntity(column));
			if ("1".equals(column.getColumnIsPrimaryKey())) {
				primaryKeyList.add("`" + column.getColumnName() + "`");
			}
			// 添加逗号，除非是最后一列
			if (i < columns.size() - 1) {
				sql.append(",\n");
			} else {
				sql.append(" ");
			}
		}
		if (primaryKeyList.isEmpty()) {
			sql.append("\n");
		} else {
			sql.append(",\n");
			sql.append("PRIMARY KEY (").append(String.join(", ", primaryKeyList)).append(") \n");
		}
//		DROP PRIMARY KEY,
//			ADD PRIMARY KEY (`XM`, `ID`) USING BTREE;
		// 添加表注释
		if (tableComment != null && !tableComment.isEmpty()) {
			sql.append(") COMMENT = '").append(tableComment).append("';\n");
		} else {
			sql.append(");\n");
		}

		return sql.toString();
	}

	/**
	 * 根据列的类型返回 SQL 类型
	 * @param column 列信息
	 * @return SQL 类型
	 */
	private static String getSqlType(ThirdPartyTableColumnEntity column) {
		if (Func.isBlank(column.getColumnType().toLowerCase())) {
			return "varchar";
		} else {
			return column.getColumnType();
		}
	}

	/**
	 * 根据列的长度和小数点位数返回大小参数
	 * @param column 列信息
	 * @return 列的大小参数
	 */
	private static String getColumnSizeAndDecimal(ThirdPartyTableColumnEntity column) {
		if ("decimal".equalsIgnoreCase(column.getColumnType())) {
			// 如果是 decimal 类型，使用列长度和小数点位数
			if (column.getColumnLong() != null && column.getColumnDecimal() != null) {
				return "(" + column.getColumnLong() + ", " + column.getColumnDecimal() + ")";
			} else {
				return "(10, 2)";
			}
			//return "(" + column.getColumnLong() + ", " + column.getColumnDecimal() + ")";
		} else if (column.getColumnLong() != null) {
			// 对于其他类型，使用列长度
			return "(" + column.getColumnLong() + ")";

		}  else if ("varchar".equals(column.getColumnType())){
			return "(255)";
		} else {
			return "";
		}
	}



	public static List<String> generateAlterTableSQL(
		String tableName,
		String newTableComment,
		List<ThirdPartyTableColumnEntity> dropList,
		List<ThirdPartyTableColumnEntity> updateOrSaveList,
		List<ThirdPartyTableColumnEntity> oldColumnList
	) {
		List<String> sqls = new ArrayList<>();
		// 存储生成的 SQL 语句




		// 1. 检查是否需要更新表注释
		StringBuilder sql1 = new StringBuilder();
		sql1.append("ALTER TABLE `").append(tableName)
			.append("` COMMENT = '").append(newTableComment).append("';\n");
		sqls.add(sql1.toString());
		// 2. 处理新增字段、删除字段、修改字段
		List<String> addColumns = new ArrayList<>();
		List<String> dropColumns = new ArrayList<>();
		List<String> modifyColumns = new ArrayList<>();

		// 3. 生成新增列的 SQL

		Map<Long,ThirdPartyTableColumnEntity> oldColumnMap = oldColumnList.stream().collect(Collectors.toMap(ThirdPartyTableColumnEntity::getId, i -> i));
		for (ThirdPartyTableColumnEntity updateOrSaveColumn : updateOrSaveList) {
			if (updateOrSaveColumn.getId() == null) {
				// 新列需要添加
				addColumns.add(generateAddColumnSQL(updateOrSaveColumn));
			} else  {
				ThirdPartyTableColumnEntity oldColumn = oldColumnMap.get(updateOrSaveColumn.getId());
				if (oldColumn == null) {
					throw new RuntimeException("未找到对应的列信息");
				}

				// 旧列和新列不一样，需要修改
				modifyColumns.add(generateModifyColumnSQL(updateOrSaveColumn,oldColumn.getColumnName()));
			}
		}

		// 4. 生成删除列的 SQL
		for (ThirdPartyTableColumnEntity oldColumn : dropList) {
			dropColumns.add("DROP COLUMN " + oldColumn.getColumnName());
		}

		// 生成删除列的 SQL

		if (!dropColumns.isEmpty()) {
			StringBuilder sql2 = new StringBuilder();
			sql2.append("ALTER TABLE `").append(tableName)
				.append("` ").append(String.join(", ", dropColumns)).append(";\n");
			sqls.add(sql2.toString());
		}

		// 生成新增列的 SQL
		if (!addColumns.isEmpty()) {
			StringBuilder sql3 = new StringBuilder();
			sql3.append("ALTER TABLE `").append(tableName)
				.append("` ").append(String.join(", ", addColumns)).append(";\n");
			sqls.add(sql3.toString());
		}



		// 生成修改列的 SQL
		if (!modifyColumns.isEmpty()) {
			StringBuilder sql4 = new StringBuilder();
			sql4.append("ALTER TABLE `").append(tableName)
				.append("` ").append(String.join(", ", modifyColumns)).append(";\n");
			sqls.add(sql4.toString());
		}
		//重新生成主键

		List<String> primaryKeyList = updateOrSaveList.stream()
			.filter(column -> "1".equals(column.getColumnIsPrimaryKey()))
			.map(i->" `"+i.getColumnName()+"` ").collect(Collectors.toList());
		List<String> oldPrimaryKeyList = oldColumnList.stream()
			.filter(column -> "1".equals(column.getColumnIsPrimaryKey()))
			.map(i->" `"+i.getColumnName()+"` ").collect(Collectors.toList());
		if (!CollectionUtils.isEqualCollection(primaryKeyList, oldPrimaryKeyList)) {
			StringBuilder sql5 = new StringBuilder();
			if (!primaryKeyList.isEmpty()) {
				sql5.append("ALTER TABLE `").append(tableName).append("` ");
				if (!oldPrimaryKeyList.isEmpty()) {
					sql5.append(" DROP PRIMARY KEY,");
				}
				sql5.append(" ADD PRIMARY KEY (")
					.append(String.join(", ", primaryKeyList)).append(") USING BTREE;\n");
			}else if (!oldPrimaryKeyList.isEmpty()){
				sql5.append("ALTER TABLE `").append(tableName).append("` ")
					.append(" DROP PRIMARY KEY;\n");

			}
			sqls.add(sql5.toString());
		}

//		sql.append("ALTER TABLE ").append(tableName)
//			.append(" DROP PRIMARY KEY,")
//			.append(" ADD PRIMARY KEY (");
		//		DROP PRIMARY KEY,
//			ADD PRIMARY KEY (`XM`, `ID`) USING BTREE;

		// 5. 重新排列列的顺序
		StringBuilder sql6 = new StringBuilder();
		sql6.append(generateReorderColumnsSQL(tableName,updateOrSaveList));
		sqls.add(sql6.toString());

		return sqls;
	}

	private static boolean columnsAreEqual(ThirdPartyTableColumnEntity oldColumn, ThirdPartyTableColumnEntity newColumn) {
		return oldColumn.getColumnType().equals(newColumn.getColumnType()) &&
			oldColumn.getColumnLong().equals(newColumn.getColumnLong()) &&
			oldColumn.getColumnDecimal().equals(newColumn.getColumnDecimal()) &&
			oldColumn.getColumnNotNull().equals(newColumn.getColumnNotNull()) &&
			oldColumn.getColumnIsPrimaryKey().equals(newColumn.getColumnIsPrimaryKey()) &&
			oldColumn.getColumnComment().equals(newColumn.getColumnComment());
	}

	private static String generateAddColumnSQL(ThirdPartyTableColumnEntity column) {
		return "ADD COLUMN " + buildColumnByEntity(column);
	}

	private static String generateModifyColumnSQL(ThirdPartyTableColumnEntity column,String oldName) {
		return "CHANGE COLUMN " + buildChangeColumnByEntity(column,oldName);
	}

	private static String generateReorderColumnsSQL(
		//List<ThirdPartyTableColumnEntity> columnOldList,
		String tableName,
		List<ThirdPartyTableColumnEntity> columnNewList) {

		StringBuilder sql = new StringBuilder();
		if (!columnNewList.isEmpty()){
			sql.append("ALTER TABLE `").append(tableName).append("` ");
		}
		// 根据 columnSort 排序列
		columnNewList.sort(Comparator.comparingInt(ThirdPartyTableColumnEntity::getColumnSort));
//		columnNewList= columnNewList.stream().peek(i->
//			i.setColumnName("`"+i.getColumnName()+"`")
//		).collect(Collectors.toList());

		for (int i = 0; i < columnNewList.size(); i++) {
			ThirdPartyTableColumnEntity column = columnNewList.get(i);
			String columnName = column.getColumnName();
			if (i == 0) {
				sql.append(" MODIFY COLUMN ")
//					.append(columnName).append("`")
//					.append(getSqlType(column))
//					.append(getColumnSizeAndDecimal(column)).append(" ")
					.append(buildColumnByEntity(column))
					.append(" FIRST");
			} else {
				sql.append(" MODIFY COLUMN ")
//					.append(columnName).append("`")
//					.append(getSqlType(column)).append(getColumnSizeAndDecimal(column)).append(" ")
					.append(buildColumnByEntity(column))
					.append(" AFTER ").append(columnNewList.get(i - 1).getColumnName());
			}

			// 需要在列与列之间添加逗号，除非是最后一个列
			if (i < columnNewList.size() - 1) {
				sql.append(", ");
			} else {
				sql.append(";\n");
			}
		}
		return sql.toString();
	}


	private static String buildColumnByEntity(ThirdPartyTableColumnEntity column) {
		StringBuilder sql = new StringBuilder();

		sql.append("    `").append(column.getColumnName()).append("` ")
			.append(getSqlType(column)).append(getColumnSizeAndDecimal(column)).append(" ");

//		// 是否为主键
//		if ("1".equals(column.getColumnIsPrimaryKey())) {
//			sql.append("PRIMARY KEY ");
//		}

		// 是否为非空字段
		if ("1".equals(column.getColumnNotNull())) {
			sql.append("NOT NULL ");
		}

		// 字段注释
		if (column.getColumnComment() != null && !column.getColumnComment().isEmpty()) {
			sql.append("COMMENT '").append(column.getColumnComment()).append("' ");
		}


		return sql.toString();
	}

	private static String buildChangeColumnByEntity(ThirdPartyTableColumnEntity column,String oldName) {
		StringBuilder sql = new StringBuilder();

		sql.append("    `").append(oldName).append("` ")
			.append("    `").append(column.getColumnName()).append("` ")

			.append(getSqlType(column)).append(getColumnSizeAndDecimal(column)).append(" ");

//		// 是否为主键
//		if ("1".equals(column.getColumnIsPrimaryKey())) {
//			sql.append("PRIMARY KEY ");
//		}

		// 是否为非空字段
		if ("1".equals(column.getColumnNotNull())) {
			sql.append("NOT NULL ");
		}

		// 字段注释
		if (column.getColumnComment() != null && !column.getColumnComment().isEmpty()) {
			sql.append("COMMENT '").append(column.getColumnComment()).append("' ");
		}


		return sql.toString();
	}
}
