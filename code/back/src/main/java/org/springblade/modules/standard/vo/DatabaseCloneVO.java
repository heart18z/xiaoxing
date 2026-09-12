package org.springblade.modules.standard.vo;

import lombok.Data;

@Data
public class DatabaseCloneVO {
	private Long sourceDataSourceId;

	private Long targetDataSourceId;

	private String sourceTableName;

	private String targetTableName;
}
