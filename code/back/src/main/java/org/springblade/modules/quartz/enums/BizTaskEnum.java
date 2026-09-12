package org.springblade.modules.quartz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BizTaskEnum {

	/**
	 * 定时任务需要在此新增,quartzTaskName内填写org.springblade.modules.quartz.task目录下面的任务的@Component("XXXX")内的名称+.方法名称
	 */
	TASK_SYNC_TEST("测试同步数据","demo","SyncDataTask.demoTask",null),
    TASK_SYNC_PEOPLE("同步人员数据","A","SyncDataTask.syncPeople","blade_people"),
	TASK_SYNC_ADDRESS("同步物业信息数据","B","SyncDataTask.syncAddress","blade_property_region"),

	TASK_SYNC_FILES("同步更新文件收纳状态","C","SyncDataTask.syncFiles",null),
	TASK_POST_ATTACH("推送附件数据实物保存","D","SyncDataTask.postAttach",null),

	TASK_SYNC_BASE_DICT("同步基础字典数据","E","SyncDataTask.syncBaseDict","blade_biz_param"),

	TASK_SYNC_SIGNPDF("同步已签署文件","F","SyncDataTask.syncSgnPdf",null),

	TASK_SYNC_BY_TEMPLATE("根据自定义模式同步数据","G","SyncDataTask.syncByTemplate",null);


    final String name;
    final String code;
	final String quartzTaskName;
	final String tableName;
}
