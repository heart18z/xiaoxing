SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `blade_ai_service_config` (
  `id` bigint NOT NULL,
  `config_name` varchar(100) NOT NULL,
  `model_alias` varchar(100) DEFAULT NULL,
  `system_default` tinyint NOT NULL DEFAULT 0,
  `base_url` varchar(500) NOT NULL,
  `api_key` text NOT NULL,
  `model_name` varchar(100) NOT NULL,
  `context_window` int NOT NULL DEFAULT 1048576,
  `max_input_tokens` int NOT NULL DEFAULT 991000,
  `max_tokens` int NOT NULL DEFAULT 131072,
  `temperature` decimal(4,2) NOT NULL DEFAULT 0.30,
  `reasoning_effort` varchar(30) NOT NULL DEFAULT 'xhigh' COMMENT '模型思考强度参数',
  `show_thinking` tinyint NOT NULL DEFAULT 1 COMMENT '是否在对话中展示思考过程',
  `request_timeout` int NOT NULL DEFAULT 120000,
  `intent_prompt` longtext,
  `decision_prompt` longtext,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `create_user` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_user` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_ai_config_enabled` (`enabled`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='智能提醒AI服务配置';

CREATE TABLE IF NOT EXISTS `blade_friend_request` (
  `id` bigint NOT NULL,
  `applicant_user_id` bigint NOT NULL,
  `target_user_id` bigint NOT NULL,
  `request_message` varchar(500) DEFAULT NULL,
  `request_type` varchar(20) NOT NULL DEFAULT 'FRIEND' COMMENT 'FRIEND好友申请/PERMISSION权限变更',
  `permission_mode` varchar(30) NOT NULL DEFAULT 'MUTUAL' COMMENT '申请人视角的提醒权限',
  `request_status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `reply_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_friend_request_target` (`target_user_id`, `request_status`),
  KEY `idx_friend_request_applicant` (`applicant_user_id`, `request_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='好友申请';

CREATE TABLE IF NOT EXISTS `blade_friendship` (
  `id` bigint NOT NULL,
  `owner_user_id` bigint NOT NULL,
  `friend_user_id` bigint NOT NULL,
  `friend_remark` varchar(100) DEFAULT NULL,
  `permission_mode` varchar(30) NOT NULL DEFAULT 'MUTUAL' COMMENT 'owner视角：I_CAN_REMIND/THEY_CAN_REMIND/MUTUAL',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_friendship_owner_friend` (`owner_user_id`, `friend_user_id`),
  KEY `idx_friendship_friend` (`friend_user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='双向好友关系';

CREATE TABLE IF NOT EXISTS `blade_smart_file` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `original_name` varchar(500) NOT NULL,
  `stored_name` varchar(200) NOT NULL,
  `file_path` varchar(1000) NOT NULL,
  `extension` varchar(20) DEFAULT NULL,
  `file_size` bigint NOT NULL DEFAULT 0,
  `extract_status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `extracted_text` longtext,
  `extract_message` varchar(1000) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_smart_file_user` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='智能提醒对话文件';

CREATE TABLE IF NOT EXISTS `blade_smart_chat_message` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `message_role` varchar(20) NOT NULL,
  `message_type` varchar(30) NOT NULL DEFAULT 'TEXT',
  `content` longtext,
  `payload_json` longtext,
  `event_id` bigint DEFAULT NULL,
  `notification_id` bigint DEFAULT NULL,
  `is_read` tinyint NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_chat_user_time` (`user_id`, `create_time`),
  KEY `idx_chat_user_unread` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户AI主对话消息';

CREATE TABLE IF NOT EXISTS `blade_smart_candidate` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `source_message_id` bigint NOT NULL,
  `original_text` longtext,
  `ai_reply` longtext,
  `event_json` longtext NOT NULL,
  `candidate_status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `confirm_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_candidate_user_status` (`user_id`, `candidate_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI识别候选事件';

CREATE TABLE IF NOT EXISTS `blade_smart_event` (
  `id` bigint NOT NULL,
  `event_no` varchar(40) NOT NULL,
  `creator_user_id` bigint NOT NULL,
  `source_candidate_id` bigint DEFAULT NULL,
  `original_text` longtext NOT NULL,
  `event_summary` varchar(1000) NOT NULL,
  `time_description` varchar(500) DEFAULT NULL,
  `event_time` datetime DEFAULT NULL,
  `deadline_time` datetime DEFAULT NULL,
  `latest_fact` longtext,
  `event_status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `ai_snapshot` longtext,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `stop_time` datetime DEFAULT NULL,
  `stop_reason` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_smart_event_no` (`event_no`),
  KEY `idx_smart_event_creator` (`creator_user_id`, `event_status`, `create_time`),
  KEY `idx_smart_event_time` (`event_time`, `event_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='智能提醒主事件';

CREATE TABLE IF NOT EXISTS `blade_smart_event_branch` (
  `id` bigint NOT NULL,
  `event_id` bigint NOT NULL,
  `recipient_user_id` bigint NOT NULL,
  `branch_status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `current_fact` longtext,
  `next_evaluate_time` datetime DEFAULT NULL,
  `last_evaluate_time` datetime DEFAULT NULL,
  `evaluate_lock` tinyint NOT NULL DEFAULT 0,
  `evaluation_version` bigint NOT NULL DEFAULT 0,
  `evaluation_requested_at` datetime DEFAULT NULL,
  `evaluation_token` varchar(36) DEFAULT NULL,
  `lock_time` datetime DEFAULT NULL,
  `last_notification_id` bigint DEFAULT NULL,
  `stop_time` datetime DEFAULT NULL,
  `stop_reason` varchar(1000) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_recipient` (`event_id`, `recipient_user_id`),
  KEY `idx_branch_due` (`branch_status`, `evaluate_lock`, `next_evaluate_time`),
  KEY `idx_branch_recipient` (`recipient_user_id`, `branch_status`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='事件接收人独立分支';

CREATE TABLE IF NOT EXISTS `blade_smart_evaluation` (
  `id` bigint NOT NULL,
  `event_id` bigint NOT NULL,
  `branch_id` bigint NOT NULL,
  `trigger_type` varchar(30) NOT NULL,
  `decision_action` varchar(30) NOT NULL,
  `decision_reason` longtext,
  `confidence` decimal(5,4) DEFAULT NULL,
  `input_snapshot` longtext,
  `output_snapshot` longtext,
  `old_evaluate_time` datetime DEFAULT NULL,
  `new_evaluate_time` datetime DEFAULT NULL,
  `model_name` varchar(100) DEFAULT NULL,
  `prompt_version` varchar(50) DEFAULT 'v1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_evaluation_event` (`event_id`, `branch_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI滚动评估记录';

CREATE TABLE IF NOT EXISTS `blade_smart_notification` (
  `id` bigint NOT NULL,
  `event_id` bigint NOT NULL,
  `branch_id` bigint NOT NULL,
  `sender_user_id` bigint NOT NULL,
  `recipient_user_id` bigint NOT NULL,
  `notification_content` longtext NOT NULL,
  `send_reason` longtext,
  `send_status` varchar(20) NOT NULL DEFAULT 'SENT',
  `plan_time` datetime DEFAULT NULL,
  `sent_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `read_time` datetime DEFAULT NULL,
  `chat_message_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notification_recipient` (`recipient_user_id`, `read_time`, `sent_time`),
  KEY `idx_notification_event` (`event_id`, `branch_id`, `sent_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='实际提醒通知';

CREATE TABLE IF NOT EXISTS `blade_smart_timeline` (
  `id` bigint NOT NULL,
  `event_id` bigint NOT NULL,
  `branch_id` bigint DEFAULT NULL,
  `actor_user_id` bigint DEFAULT NULL,
  `node_type` varchar(30) NOT NULL,
  `content` longtext,
  `payload_json` longtext,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_timeline_event` (`event_id`, `branch_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='事件完整时间轴';

INSERT INTO `blade_role` (`id`, `tenant_id`, `parent_id`, `role_name`, `sort`, `role_alias`, `is_deleted`, `role_type`, `remark`, `purpose`)
SELECT 2099000000000000001, '000000', 0, 'APP端使用人员', 30, 'app_user', 0, 'menu', '允许登录智能提醒移动端', 'mobile'
WHERE NOT EXISTS (SELECT 1 FROM `blade_role` WHERE `role_alias` = 'app_user' AND `is_deleted` = 0);

UPDATE `blade_user`
SET `role_id` = CONCAT_WS(',', NULLIF(`role_id`, ''), '2099000000000000001')
WHERE `account` IN ('adminer', 'adm', 'produ')
  AND FIND_IN_SET('2099000000000000001', `role_id`) = 0;

INSERT INTO `blade_menu` (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`, `category`, `action`, `is_open`, `component`, `remark`, `is_deleted`)
SELECT 2099000000000000100, 1123598815738675203, 'aiServiceConfig', 'AI服务配置', 'menu', '/smartReminder/aiConfig', 'iconfont iconicon_setting', 31, 1, 0, 1, NULL, 'NewAPI/OpenAI兼容接口配置', 0
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `code` = 'aiServiceConfig' AND `is_deleted` = 0);

SET FOREIGN_KEY_CHECKS = 1;
