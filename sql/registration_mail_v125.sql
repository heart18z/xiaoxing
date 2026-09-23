SET NAMES utf8mb4;
-- Additive schema is initialized by RegistrationMailSchema. No existing accounts change.
INSERT INTO blade_menu (id,parent_id,code,name,alias,path,source,sort,category,action,is_open,component,remark,is_deleted)
SELECT 2099000000000000125,2099000000000000090,'registrationMail','注册邮箱验证','menu','/smartReminder/registrationMail','iconfont iconicon_setting',7,1,0,1,NULL,'动态SMTP配置与测试',0
WHERE NOT EXISTS (SELECT 1 FROM blade_menu WHERE id=2099000000000000125);
INSERT INTO blade_role_menu(id,menu_id,role_id)
SELECT 2099000000000001125,2099000000000000125,1123598816738675201
WHERE NOT EXISTS (SELECT 1 FROM blade_role_menu WHERE menu_id=2099000000000000125 AND role_id=1123598816738675201);
