package org.springblade.modules.quartz.support;

import lombok.Data;

import java.util.Date;
import java.util.List;
@Data
public class FilesSyncVO {
	private String systemId;

	private Date syncUpdateTime;

	private String filesNum;

	private Long sourceId;

	List<FileItem> fileList;
}
