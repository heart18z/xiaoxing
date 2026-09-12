package org.springblade.modules.quartz.support;

import lombok.Data;

@Data
public class FilesStoreVO {

	private String storeCode;

	private String fullStoreLocation;

	private String keeperName;

	private Long sourceId;

	/**
	 * 保管状态
	 */
	private String safekeepStatus;

}
