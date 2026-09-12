package org.springblade.modules.quartz.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.NullSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.tenant.mp.TenantEntity;
import org.springblade.modules.quartz.constants.ScheduleConstants;

import java.io.Serializable;


/**
 * 定时任务调度表 blade_job
 *
 * @author pc
 */
@Data
@TableName("blade_job")
@EqualsAndHashCode(callSuper = true)
public class SysJob extends TenantEntity
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */

    private Long id;

    /** 任务名称 */

    private String jobName;

    /** 任务组名 */
    private String jobGroup;

    /** 调用目标字符串 */
    private String invokeTarget;

    /** cron执行表达式 */
    private String cronExpression;

    /** cron计划策略 */
    private String misfirePolicy;

    /** 是否并发执行（0允许 1禁止） */
    private String concurrent;

    /** 任务状态（0正常 1暂停） */
    private String processStatus;


	private Integer isExternal;
	@JsonSerialize(nullsUsing = NullSerializer.class)
	private Long interactionId;

	private String jobRunTemplate;

	private String jobCode;




}
