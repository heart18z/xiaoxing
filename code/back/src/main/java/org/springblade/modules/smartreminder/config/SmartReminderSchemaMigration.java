package org.springblade.modules.smartreminder.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 智能提醒模块的小步兼容迁移。项目没有引入 Flyway，因而只在字段缺失时补齐，
 * 既能升级现有部署，也不会覆盖已经存在的业务数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmartReminderSchemaMigration {

	private final JdbcTemplate jdbcTemplate;

	@EventListener(ApplicationReadyEvent.class)
	public void migrate() {
		org.springblade.modules.smartreminder.push.PushSchema.migrate(jdbcTemplate);
		for(String column:java.util.List.of("overview_summary text","overview_source_hash varchar(64)","overview_updated_at datetime(6)","overview_retry_after datetime"))
			addColumnIfMissing("blade_smart_event",column.split(" ")[0],"alter table blade_smart_event add column "+column+" null");
		jdbcTemplate.execute("""
			create table if not exists blade_smart_person_alias (
			 id bigint primary key, owner_user_id bigint not null, person_user_id bigint not null,
			 alias_name varchar(100) not null, source_message_id bigint null,
			 create_time datetime default current_timestamp, update_time datetime default current_timestamp,
			 unique key uk_owner_alias(owner_user_id,alias_name), key idx_alias_person(person_user_id)
			) comment='当前用户对人员的专属别称记忆'
			""");
		jdbcTemplate.execute("""
			create table if not exists blade_smart_user_preference (
			 user_id bigint primary key, ai_avatar varchar(1000) null, update_time datetime default current_timestamp
			) comment='用户专属AI外观设置'
			""");
		addColumnIfMissing("blade_smart_event_branch", "task_content",
			"alter table blade_smart_event_branch add column task_content text null comment '仅面向本接收人的事项' ");
		addColumnIfMissing("blade_smart_event", "conversation_context_json",
			"alter table blade_smart_event add column conversation_context_json mediumtext null comment 'AI筛选的创建上下文消息ID' ");
		jdbcTemplate.execute("""
			create table if not exists blade_smart_message_event (
			 event_id bigint not null, user_id bigint not null, message_id bigint not null,
			 primary key(event_id,user_id,message_id), key idx_message(message_id)
			) comment='事件与原始对话消息关联'
			""");
		addColumnIfMissing("blade_ai_service_config", "reasoning_effort",
			"alter table blade_ai_service_config add column reasoning_effort varchar(30) not null default 'xhigh' comment '模型思考强度参数' after temperature");
		addColumnIfMissing("blade_ai_service_config", "show_thinking",
			"alter table blade_ai_service_config add column show_thinking tinyint not null default 1 comment '是否在对话中展示思考过程' after reasoning_effort");
		addColumnIfMissing("blade_ai_service_config", "config_type",
			"alter table blade_ai_service_config add column config_type varchar(20) not null default 'LLM' comment 'LLM或SPEECH'");
		addColumnIfMissing("blade_ai_service_config", "model_alias",
			"alter table blade_ai_service_config add column model_alias varchar(100) null");
		addColumnIfMissing("blade_ai_service_config", "system_default",
			"alter table blade_ai_service_config add column system_default tinyint not null default 0");
		addColumnIfMissing("blade_smart_event_branch", "evaluation_version",
			"alter table blade_smart_event_branch add column evaluation_version bigint not null default 0");
		addColumnIfMissing("blade_smart_event_branch", "evaluation_requested_at",
			"alter table blade_smart_event_branch add column evaluation_requested_at datetime null");
		addColumnIfMissing("blade_smart_event_branch", "evaluation_token",
			"alter table blade_smart_event_branch add column evaluation_token varchar(36) null");
		addColumnIfMissing("blade_smart_user_preference", "llm_config_id",
			"alter table blade_smart_user_preference add column llm_config_id bigint null");
		addColumnIfMissing("blade_smart_user_preference", "chat_context_start_id",
			"alter table blade_smart_user_preference add column chat_context_start_id bigint not null default 0");
		addColumnIfMissing("blade_smart_event_branch", "latest_summary",
			"alter table blade_smart_event_branch add column latest_summary text null");
		addColumnIfMissing("blade_smart_event_branch", "task_time_scoped",
			"alter table blade_smart_event_branch add column task_time_scoped tinyint not null default 0");
		addColumnIfMissing("blade_smart_event_branch", "task_event_time",
			"alter table blade_smart_event_branch add column task_event_time datetime null");
		addColumnIfMissing("blade_smart_event_branch", "task_deadline_time",
			"alter table blade_smart_event_branch add column task_deadline_time datetime null");
		addColumnIfMissing("blade_smart_event_branch", "summary_source_hash",
			"alter table blade_smart_event_branch add column summary_source_hash varchar(64) null");
		addColumnIfMissing("blade_smart_event_branch", "summary_updated_at",
			"alter table blade_smart_event_branch add column summary_updated_at datetime(6) null");
		addColumnIfMissing("blade_smart_event_branch", "summary_retry_after",
			"alter table blade_smart_event_branch add column summary_retry_after datetime null");
		addColumnIfMissing("blade_smart_user_preference", "speech_config_id",
			"alter table blade_smart_user_preference add column speech_config_id bigint null");
		Integer bodyColumn=jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema=database() and table_name='blade_ai_service_config' and column_name='extra_body'",Integer.class);
		if(bodyColumn==0){
			jdbcTemplate.execute("alter table blade_ai_service_config add column extra_body text null");
			jdbcTemplate.execute("update blade_ai_service_config set extra_body=case when config_type='LLM' and coalesce(reasoning_effort,'')<>'' then JSON_OBJECT('reasoning_effort',reasoning_effort) else '{}' end");
		}
		addColumnIfMissing("blade_smart_user_preference","llm_mode","alter table blade_smart_user_preference add column llm_mode varchar(20) not null default 'SYSTEM'");
		addColumnIfMissing("blade_smart_user_preference","speech_mode","alter table blade_smart_user_preference add column speech_mode varchar(20) not null default 'SYSTEM'");
		addColumnIfMissing("blade_smart_user_preference","llm_personal","alter table blade_smart_user_preference add column llm_personal mediumtext null");
		addColumnIfMissing("blade_smart_user_preference","speech_personal","alter table blade_smart_user_preference add column speech_personal mediumtext null");
		addColumnIfMissing("blade_smart_user_preference","language","alter table blade_smart_user_preference add column language varchar(10) not null default 'zh-cn'");
		jdbcTemplate.update("update blade_menu set name='模型配置' where name in ('AI服务配置','AI 服务配置') and is_deleted=0");
		jdbcTemplate.execute("create table if not exists blade_smart_model_policy(config_type varchar(20) primary key,intent_prompt mediumtext null,decision_prompt mediumtext null,update_time datetime default current_timestamp)");
		for (String type : java.util.List.of("LLM", "SPEECH")) {
			jdbcTemplate.update("insert ignore into blade_smart_model_policy(config_type,intent_prompt,decision_prompt) select ?,intent_prompt,decision_prompt from blade_ai_service_config where config_type=? and is_deleted=0 order by enabled desc,update_time desc,id desc limit 1",type,type);
			jdbcTemplate.update("insert ignore into blade_smart_model_policy(config_type) values(?)",type);
			var active=jdbcTemplate.query("select id from blade_ai_service_config where config_type=? and enabled=1 and is_deleted=0 order by update_time desc,id desc",(rs,n)->rs.getLong(1),type);
			if(!active.isEmpty() && jdbcTemplate.queryForObject("select count(*) from blade_ai_service_config where config_type=? and enabled=1 and system_default=1 and is_deleted=0",Integer.class,type)==0)
				jdbcTemplate.update("update blade_ai_service_config set system_default=case when id=? then 1 else 0 end where config_type=? and is_deleted=0",active.get(0),type);
		}
		addIndexIfMissing("blade_smart_chat_message","idx_chat_event_user","create index idx_chat_event_user on blade_smart_chat_message(user_id,event_id,create_time,id)");
		addIndexIfMissing("blade_smart_event_branch","idx_branch_requested","create index idx_branch_requested on blade_smart_event_branch(branch_status,evaluate_lock,evaluation_requested_at)");
		addIndexIfMissing("blade_smart_chat_message","idx_chat_user_role_id","create index idx_chat_user_role_id on blade_smart_chat_message(user_id,message_role,id)");
		addIndexIfMissing("blade_smart_timeline","idx_timeline_event_actor_time","create index idx_timeline_event_actor_time on blade_smart_timeline(event_id,actor_user_id,create_time)");
		addColumnIfMissing("blade_friend_request", "request_type",
			"alter table blade_friend_request add column request_type varchar(20) not null default 'FRIEND' comment 'FRIEND好友申请/PERMISSION权限变更' after request_message");
		addColumnIfMissing("blade_friend_request", "permission_mode",
			"alter table blade_friend_request add column permission_mode varchar(30) not null default 'MUTUAL' comment '申请人视角的提醒权限' after request_type");
		addColumnIfMissing("blade_friendship", "permission_mode",
			"alter table blade_friendship add column permission_mode varchar(30) not null default 'MUTUAL' comment 'owner视角提醒权限' after friend_remark");
	}

	private void addIndexIfMissing(String table,String index,String ddl) {
		Integer count=jdbcTemplate.queryForObject("select count(*) from information_schema.statistics where table_schema=database() and table_name=? and index_name=?",Integer.class,table,index);
		if(count==0)jdbcTemplate.execute(ddl);
	}

	private void addColumnIfMissing(String table, String column, String ddl) {
		Integer count = jdbcTemplate.queryForObject("select count(*) from information_schema.columns where table_schema=database() and table_name=? and column_name=?",
			Integer.class, table, column);
		if (count != null && count == 0) {
			jdbcTemplate.execute(ddl);
			log.info("Smart reminder schema upgraded: {}.{}", table, column);
		}
	}
}
