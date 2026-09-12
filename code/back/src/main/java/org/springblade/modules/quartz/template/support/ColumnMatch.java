package org.springblade.modules.quartz.template.support;

import lombok.Data;

@Data
public class ColumnMatch {

	private String tableColumn;
	private String targetProp;
//	private Boolean isPk;
}
