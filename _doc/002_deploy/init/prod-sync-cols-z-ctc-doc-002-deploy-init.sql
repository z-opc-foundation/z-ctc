-- =====================================================================
-- 结构对齐回写 (缺列) — 生成日 2026-10-24
-- 来源: 生产 oc 只读 mysqldump --no-data
-- 口径: 只加不改; 只收「两种独立解析实现都判缺」的列, 歧义区不动。
-- 用法: mysql <库名> < 本文件 (脚本用 DELIMITER, 走 mysql 客户端执行)。
-- 位置: 与本表 CREATE 语句同目录 (z-ctc/_doc/002_deploy/init)
-- =====================================================================

-- 逐列补齐: 只在列不存在时才 ALTER ⇒ 可重复执行。
-- MySQL **不支持** ALTER TABLE ... ADD COLUMN IF NOT EXISTS (那是 MariaDB 语法),
-- 所以幂等靠 information_schema 判定 + 动态 SQL, 不靠语法糖。
DROP PROCEDURE IF EXISTS `zopc_sync_prod_cols_z-ctc-doc-002-deploy-init`;
DELIMITER $$
CREATE PROCEDURE `zopc_sync_prod_cols_z-ctc-doc-002-deploy-init`()
BEGIN
  DECLARE v_t VARCHAR(64);
  DECLARE v_c VARCHAR(64);
  DECLARE v_d TEXT;
  DECLARE cur CURSOR FOR SELECT tbl, col, defn FROM `_zopc_prod_sync_cols_z-ctc-doc-002-deploy-init`;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_t = NULL;
  OPEN cur;
  lp: LOOP
    FETCH cur INTO v_t, v_c, v_d;
    IF v_t IS NULL THEN LEAVE lp; END IF;
    IF EXISTS (
         SELECT 1 FROM information_schema.TABLES
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = v_t)
       AND NOT EXISTS (
         SELECT 1 FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = v_t AND COLUMN_NAME = v_c) THEN
      SET @ddl = CONCAT('ALTER TABLE `', v_t, '` ADD COLUMN `', v_c, '` ', v_d);
      PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

DROP TEMPORARY TABLE IF EXISTS `_zopc_prod_sync_cols_z-ctc-doc-002-deploy-init`;
CREATE TEMPORARY TABLE `_zopc_prod_sync_cols_z-ctc-doc-002-deploy-init` (tbl VARCHAR(64), col VARCHAR(64), defn TEXT);
INSERT INTO `_zopc_prod_sync_cols_z-ctc-doc-002-deploy-init` (tbl, col, defn) VALUES
  ('z_ctc_ac_account', 'domain_code', 'varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL'),
  ('z_ctc_ac_auth', 'account_id', 'bigint DEFAULT NULL COMMENT ''账号 ID (关联 z_ctc_ac_account.id)'''),
  ('z_ctc_ac_auth', 'domain_code', 'varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL'),
  ('z_ctc_ac_auth', 'tenant_code', 'varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL'),
  ('z_ctc_ac_login_log', 'domain_code', 'varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL'),
  ('z_ctc_ac_tenant', 'domain_code', 'varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL'),
  ('z_ctc_app_menu', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_ctc_app_menu', 'gmt_create', 'datetime DEFAULT CURRENT_TIMESTAMP'),
  ('z_ctc_app_menu', 'gmt_modified', 'datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'),
  ('z_ctc_app_menu', 'permission_code', 'varchar(128) DEFAULT NULL'),
  ('z_ctc_app_menu', 'tenant_code', 'varchar(64) DEFAULT NULL'),
  ('z_ctc_spc_resource', 'domain_code', 'varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL'),
  ('z_ctc_spc_role', 'domain_code', 'varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL'),
  ('z_ctc_spc_role_resource', 'domain_code', 'varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL'),
  ('z_ctc_spc_role_resource', 'tenant_code', 'varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL'),
  ('z_ctc_spc_user_role', 'domain_code', 'varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL'),
  ('z_ctc_surl_mapping', 'domain_code', 'varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL'),
  ('z_ctc_tenant', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_ctc_tenant', 'ext_config', 'text COMMENT ''扩展K-V配置(JSON)'''),
  ('z_ctc_verify_code', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_ctc_verify_code', 'tenant_code', 'varchar(64) DEFAULT NULL');

CALL `zopc_sync_prod_cols_z-ctc-doc-002-deploy-init`();
DROP PROCEDURE IF EXISTS `zopc_sync_prod_cols_z-ctc-doc-002-deploy-init`;
DROP TEMPORARY TABLE IF EXISTS `_zopc_prod_sync_cols_z-ctc-doc-002-deploy-init`;
