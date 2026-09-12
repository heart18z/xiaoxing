package org.springblade.modules.quartz.domain;

import lombok.Data;

import java.util.Date;

@Data
public class SysJobSchedule {

	private String jobName;

	private String runTime;

	private Long jobId;

}
