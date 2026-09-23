SET NAMES utf8mb4;
SET @has_file_name = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='blade_app_release' AND column_name='file_name');
SET @ddl = IF(@has_file_name=0, 'ALTER TABLE blade_app_release ADD COLUMN file_name varchar(255) NULL AFTER notes', 'SELECT 1');
PREPARE app_release_ddl FROM @ddl;
EXECUTE app_release_ddl;
DEALLOCATE PREPARE app_release_ddl;
