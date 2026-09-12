package org.springblade.modules.quartz.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SysJobVO extends SysJob{
	private String tableName;
}
