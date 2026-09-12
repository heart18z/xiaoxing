package org.springblade.modules.quartz.constants;

/**
 * 任务调度通用常量
 *
 * @author pc
 */
public class ScheduleConstants
{
    public static final String TASK_CLASS_NAME = "TASK_CLASS_NAME";

    /** 执行目标key */
    public static final String TASK_PROPERTIES = "TASK_PROPERTIES";

    /** 默认 */
    public static final String MISFIRE_DEFAULT = "0";

    /** 立即触发执行 */
    public static final String MISFIRE_IGNORE_MISFIRES = "1";

    /** 触发一次执行 */
    public static final String MISFIRE_FIRE_AND_PROCEED = "2";

    /** 不触发立即执行 */
    public static final String MISFIRE_DO_NOTHING = "3";

    public enum Status
    {
        /**
         * 正常
         */
        NORMAL("0"),
        /**
         * 暂停
         */
        PAUSE("1");

        private String value;

        private Status(String value)
        {
            this.value = value;
        }

        public String getValue()
        {
            return value;
        }
    }

	/**
	 * 通用成功标识
	 */
	public static final String SUCCESS = "0";

	/**
	 * 通用失败标识
	 */
	public static final String FAIL = "1";


	/**
	 * 定时任务白名单配置（仅允许访问的包名，如其他需要可以自行添加）
	 */
	public static final String[] JOB_WHITELIST_STR = { "org.springblade" };

	/**
	 * 定时任务违规的字符
	 */
	public static final String[] JOB_ERROR_STR = { "java.net.URL", "javax.naming.InitialContext", "org.yaml.snakeyaml",
		"org.springframework", "org.apache", "com.pc.common.utils.file", "org.springblade.common.config" };


	/**
	 * RMI 远程方法调用
	 */
	public static final String LOOKUP_RMI = "rmi:";

	/**
	 * LDAP 远程方法调用
	 */
	public static final String LOOKUP_LDAP = "ldap:";

	/**
	 * LDAPS 远程方法调用
	 */
	public static final String LOOKUP_LDAPS = "ldaps:";

	/**
	 * http请求
	 */
	public static final String HTTP = "http://";

	/**
	 * https请求
	 */
	public static final String HTTPS = "https://";

	public static final String DATA_SOURCE_SYNC="sync";

	public static final String DATA_SOURCE_ADD="add";
}
