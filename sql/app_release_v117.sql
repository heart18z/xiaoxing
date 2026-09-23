SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS blade_app_release (
 id bigint NOT NULL PRIMARY KEY, version_code int NOT NULL, version_name varchar(40) NOT NULL,
 notes text NOT NULL, file_size bigint NOT NULL, sha256 char(64) NOT NULL,
 published tinyint NOT NULL DEFAULT 0, create_user bigint, create_time datetime NOT NULL,
 UNIQUE KEY uk_app_release_version(version_code)
);
INSERT INTO blade_menu (id,parent_id,code,name,alias,path,source,sort,category,action,is_open,component,remark,is_deleted)
SELECT 2099000000000000117,2099000000000000090,'appReleases','APP版本管理','menu','/smartReminder/releases','iconfont iconicon_setting',6,1,0,1,NULL,'安卓APK上传与发布',0
WHERE NOT EXISTS (SELECT 1 FROM blade_menu WHERE id=2099000000000000117);
INSERT INTO blade_role_menu(id,menu_id,role_id)
SELECT 2099000000000001117,2099000000000000117,1123598816738675201
WHERE NOT EXISTS (SELECT 1 FROM blade_role_menu WHERE menu_id=2099000000000000117 AND role_id=1123598816738675201);
