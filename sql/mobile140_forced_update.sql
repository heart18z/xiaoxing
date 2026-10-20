-- 140: additive migration. Existing releases remain optional by default.
-- The application also adds this column on startup if it is missing.
SET NAMES utf8mb4;
SET @mobile140_column_exists = (
 SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'blade_app_release' AND column_name = 'force_update'
);
SET @mobile140_ddl = IF(@mobile140_column_exists = 0,
 'ALTER TABLE blade_app_release ADD COLUMN force_update TINYINT NOT NULL DEFAULT 0 COMMENT ''是否强制更新''',
 'SELECT 1');
PREPARE mobile140_statement FROM @mobile140_ddl;
EXECUTE mobile140_statement;
DEALLOCATE PREPARE mobile140_statement;
