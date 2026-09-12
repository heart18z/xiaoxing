package org.springblade.modules.quartz.template.support;

import lombok.Data;

import java.util.List;
import java.util.Map;
@Data
public class TaskInfo {
	private String url;
	private String method;
	private String res;
	private String currentPage;
	private String pageSize;


	private String updateColumn;
	private String tablePk;
	private Boolean isNeedCreatedId;

	private String tableName;

	private Map<String, String> headers;

	private Map<String, Object> params;

	private List<ColumnMatch> columnMatches;
}
