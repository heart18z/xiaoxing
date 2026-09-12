SET NAMES utf8mb4;

ALTER TABLE `blade_ai_service_config`
  ADD COLUMN `context_window` int NOT NULL DEFAULT 1048576 AFTER `model_name`,
  ADD COLUMN `max_input_tokens` int NOT NULL DEFAULT 991000 AFTER `context_window`;

UPDATE `blade_ai_service_config`
SET `context_window`=1048576, `max_input_tokens`=991000, `max_tokens`=131072
WHERE `is_deleted`=0;

INSERT INTO `blade_menu` (`id`,`parent_id`,`code`,`name`,`alias`,`path`,`source`,`sort`,`category`,`action`,`is_open`,`component`,`remark`,`is_deleted`)
SELECT 2099000000000000090,0,'smartReminder','智能提醒','menu','/smartReminder','iconfont iconicon_community_line',9999,1,0,1,'','AI智能提醒后台管理',0
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `id`=2099000000000000090);

INSERT INTO `blade_menu` (`id`,`parent_id`,`code`,`name`,`alias`,`path`,`source`,`sort`,`category`,`action`,`is_open`,`component`,`remark`,`is_deleted`)
SELECT 2099000000000000101,2099000000000000090,'smartReminderOverview','智能提醒概览','menu','/smartReminder/overview','iconfont iconicon_airplay',1,1,0,1,NULL,'智能提醒业务概览',0
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `id`=2099000000000000101);

INSERT INTO `blade_menu` (`id`,`parent_id`,`code`,`name`,`alias`,`path`,`source`,`sort`,`category`,`action`,`is_open`,`component`,`remark`,`is_deleted`)
SELECT 2099000000000000102,2099000000000000090,'smartReminderEvents','事件记录','menu','/smartReminder/events','iconfont iconicon_savememo',2,1,0,1,NULL,'智能提醒事件记录',0
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `id`=2099000000000000102);

INSERT INTO `blade_menu` (`id`,`parent_id`,`code`,`name`,`alias`,`path`,`source`,`sort`,`category`,`action`,`is_open`,`component`,`remark`,`is_deleted`)
SELECT 2099000000000000103,2099000000000000090,'smartReminderNotifications','提醒通知','menu','/smartReminder/notifications','iconfont iconicon_sms',3,1,0,1,NULL,'智能提醒通知发送记录',0
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `id`=2099000000000000103);

INSERT INTO `blade_menu` (`id`,`parent_id`,`code`,`name`,`alias`,`path`,`source`,`sort`,`category`,`action`,`is_open`,`component`,`remark`,`is_deleted`)
SELECT 2099000000000000104,2099000000000000090,'smartReminderEvaluations','AI评估记录','menu','/smartReminder/evaluations','iconfont iconicon_study',4,1,0,1,NULL,'AI滚动评估决策记录',0
WHERE NOT EXISTS (SELECT 1 FROM `blade_menu` WHERE `id`=2099000000000000104);

UPDATE `blade_menu`
SET `parent_id`=2099000000000000090,`sort`=5,`name`='AI服务配置',`is_deleted`=0
WHERE `id`=2099000000000000100;

UPDATE `blade_menu` SET `sort`=9999 WHERE `id`=2099000000000000090;
UPDATE `blade_menu` SET `source`='iconfont iconicon_savememo' WHERE `id`=2099000000000000102;

INSERT INTO `blade_role_menu` (`id`,`menu_id`,`role_id`)
SELECT 2099000000000001000,2099000000000000090,1123598816738675201
WHERE NOT EXISTS (SELECT 1 FROM `blade_role_menu` WHERE `menu_id`=2099000000000000090 AND `role_id`=1123598816738675201);
INSERT INTO `blade_role_menu` (`id`,`menu_id`,`role_id`)
SELECT 2099000000000001001,2099000000000000101,1123598816738675201
WHERE NOT EXISTS (SELECT 1 FROM `blade_role_menu` WHERE `menu_id`=2099000000000000101 AND `role_id`=1123598816738675201);
INSERT INTO `blade_role_menu` (`id`,`menu_id`,`role_id`)
SELECT 2099000000000001002,2099000000000000102,1123598816738675201
WHERE NOT EXISTS (SELECT 1 FROM `blade_role_menu` WHERE `menu_id`=2099000000000000102 AND `role_id`=1123598816738675201);
INSERT INTO `blade_role_menu` (`id`,`menu_id`,`role_id`)
SELECT 2099000000000001003,2099000000000000103,1123598816738675201
WHERE NOT EXISTS (SELECT 1 FROM `blade_role_menu` WHERE `menu_id`=2099000000000000103 AND `role_id`=1123598816738675201);
INSERT INTO `blade_role_menu` (`id`,`menu_id`,`role_id`)
SELECT 2099000000000001004,2099000000000000104,1123598816738675201
WHERE NOT EXISTS (SELECT 1 FROM `blade_role_menu` WHERE `menu_id`=2099000000000000104 AND `role_id`=1123598816738675201);
INSERT INTO `blade_role_menu` (`id`,`menu_id`,`role_id`)
SELECT 2099000000000001005,2099000000000000100,1123598816738675201
WHERE NOT EXISTS (SELECT 1 FROM `blade_role_menu` WHERE `menu_id`=2099000000000000100 AND `role_id`=1123598816738675201);
