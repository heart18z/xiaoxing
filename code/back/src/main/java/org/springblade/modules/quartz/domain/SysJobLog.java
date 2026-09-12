package org.springblade.modules.quartz.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.BaseEntity;
import org.springblade.core.tenant.mp.TenantEntity;


import java.io.Serializable;
import java.util.Date;

/**
 * 定时任务调度日志表 blade_job_log
 *
 * @author pc
 */
@Data
@TableName("blade_job_log")
public class SysJobLog  implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** ID */

    private Long jobLogId;

	private Long jobId;

    /** 任务名称 */

    private String jobName;

    /** 任务组名 */

    private String jobGroup;

    /** 调用目标字符串 */

    private String invokeTarget;

    /** 日志信息 */

    private String jobMessage;

    /** 执行状态（0正常 1失败） */

    private String status;

    /** 异常信息 */

    private String exceptionInfo;

    /** 开始时间 */
    private Date startTime;

    /** 结束时间 */
    private Date endTime;


}
