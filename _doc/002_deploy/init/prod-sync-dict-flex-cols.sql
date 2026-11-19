-- =====================================================================
-- 字典表弹性口径迁移 — 生成日 2026-11-19
-- 对象: 生产 oc 的 z_ctc_dict_category / z_ctc_dictionary
--       (2026-10-24 由 z_dict_* 迁入的旧口径: gmt_create / ext_data / parent_id, 无弹性列)
-- 口径: 列名统一到 z-ctc 实体约定 (created_at/updated_at/ext_props/parent_key),
--       补齐弹性模型列 (scope/value_type/entry_shape/ext_schema/builtin/渲染提示/生效窗口)。
--       全程幂等: 每步先查 information_schema, 已存在即跳过; 只改列名不加不减数据。
-- 用法: mysql <库名> < 本文件
-- =====================================================================

DROP PROCEDURE IF EXISTS `zopc_migrate_dict_flex`;

DELIMITER $$
CREATE PROCEDURE `zopc_migrate_dict_flex`()
BEGIN
  DECLARE v_ddl TEXT;

  -- ---------- helper: 列不存在则执行 DDL ----------
  -- 用局部过程不便, 直接展开判定 (MySQL 过程内无嵌套过程), 每列一段。

  -- ===== z_ctc_dict_category =====

  -- gmt_create → created_at (house style, 见 z_ctc_ac_domain)
  IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dict_category'
                AND COLUMN_NAME = 'gmt_create')
     AND NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dict_category'
                AND COLUMN_NAME = 'created_at') THEN
    ALTER TABLE `z_ctc_dict_category` RENAME COLUMN `gmt_create` TO `created_at`;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dict_category'
                AND COLUMN_NAME = 'updated_at') THEN
    ALTER TABLE `z_ctc_dict_category` ADD COLUMN `updated_at` datetime DEFAULT NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dict_category'
                AND COLUMN_NAME = 'created_by') THEN
    ALTER TABLE `z_ctc_dict_category` ADD COLUMN `created_by` varchar(64) DEFAULT NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dict_category'
                AND COLUMN_NAME = 'updated_by') THEN
    ALTER TABLE `z_ctc_dict_category` ADD COLUMN `updated_by` varchar(64) DEFAULT NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dict_category'
                AND COLUMN_NAME = 'scope') THEN
    ALTER TABLE `z_ctc_dict_category` ADD COLUMN `scope` varchar(16) DEFAULT 'TENANT';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dict_category'
                AND COLUMN_NAME = 'value_type') THEN
    ALTER TABLE `z_ctc_dict_category` ADD COLUMN `value_type` varchar(16) DEFAULT 'string';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dict_category'
                AND COLUMN_NAME = 'entry_shape') THEN
    ALTER TABLE `z_ctc_dict_category` ADD COLUMN `entry_shape` varchar(8) DEFAULT 'flat';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dict_category'
                AND COLUMN_NAME = 'ext_schema') THEN
    ALTER TABLE `z_ctc_dict_category` ADD COLUMN `ext_schema` text;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dict_category'
                AND COLUMN_NAME = 'builtin') THEN
    ALTER TABLE `z_ctc_dict_category` ADD COLUMN `builtin` tinyint DEFAULT 0;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dict_category'
                AND COLUMN_NAME = 'status') THEN
    ALTER TABLE `z_ctc_dict_category` ADD COLUMN `status` tinyint DEFAULT 1;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dict_category'
                AND COLUMN_NAME = 'description') THEN
    ALTER TABLE `z_ctc_dict_category` ADD COLUMN `description` varchar(512) DEFAULT NULL;
  END IF;

  -- 旧行回填: scope 按 domain_code 推导; 空值归默认
  UPDATE `z_ctc_dict_category` SET `scope` = 'DOMAIN' WHERE `domain_code` IS NOT NULL AND `domain_code` <> '' AND (`scope` IS NULL OR `scope` = '');
  UPDATE `z_ctc_dict_category` SET `scope` = 'TENANT' WHERE (`scope` IS NULL OR `scope` = '');
  UPDATE `z_ctc_dict_category` SET `status` = 1 WHERE `status` IS NULL;
  UPDATE `z_ctc_dict_category` SET `value_type` = 'string' WHERE `value_type` IS NULL OR `value_type` = '';
  UPDATE `z_ctc_dict_category` SET `entry_shape` = 'flat' WHERE `entry_shape` IS NULL OR `entry_shape` = '';
  UPDATE `z_ctc_dict_category` SET `builtin` = 0 WHERE `builtin` IS NULL;

  -- ===== z_ctc_dictionary =====

  IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dictionary'
                AND COLUMN_NAME = 'gmt_create')
     AND NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dictionary'
                AND COLUMN_NAME = 'created_at') THEN
    ALTER TABLE `z_ctc_dictionary` RENAME COLUMN `gmt_create` TO `created_at`;
  END IF;

  IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dictionary'
                AND COLUMN_NAME = 'gmt_modified')
     AND NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dictionary'
                AND COLUMN_NAME = 'updated_at') THEN
    ALTER TABLE `z_ctc_dictionary` RENAME COLUMN `gmt_modified` TO `updated_at`;
  END IF;

  -- ext_data → ext_props (语义一致, 旧列全空, 统一到实体命名)
  IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dictionary'
                AND COLUMN_NAME = 'ext_data')
     AND NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dictionary'
                AND COLUMN_NAME = 'ext_props') THEN
    ALTER TABLE `z_ctc_dictionary` RENAME COLUMN `ext_data` TO `ext_props`;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dictionary'
                AND COLUMN_NAME = 'data_value') THEN
    ALTER TABLE `z_ctc_dictionary` ADD COLUMN `data_value` varchar(512) DEFAULT NULL;
  END IF;

  -- tree 语义用 parent_key (跨作用域可引用); 旧 parent_id 全空, 留置不删
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dictionary'
                AND COLUMN_NAME = 'parent_key') THEN
    ALTER TABLE `z_ctc_dictionary` ADD COLUMN `parent_key` varchar(128) DEFAULT NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dictionary'
                AND COLUMN_NAME = 'tags') THEN
    ALTER TABLE `z_ctc_dictionary` ADD COLUMN `tags` varchar(255) DEFAULT NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dictionary'
                AND COLUMN_NAME = 'icon') THEN
    ALTER TABLE `z_ctc_dictionary` ADD COLUMN `icon` varchar(64) DEFAULT NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dictionary'
                AND COLUMN_NAME = 'color') THEN
    ALTER TABLE `z_ctc_dictionary` ADD COLUMN `color` varchar(32) DEFAULT NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dictionary'
                AND COLUMN_NAME = 'ref_category') THEN
    ALTER TABLE `z_ctc_dictionary` ADD COLUMN `ref_category` varchar(64) DEFAULT NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dictionary'
                AND COLUMN_NAME = 'ref_key') THEN
    ALTER TABLE `z_ctc_dictionary` ADD COLUMN `ref_key` varchar(128) DEFAULT NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dictionary'
                AND COLUMN_NAME = 'effective_from') THEN
    ALTER TABLE `z_ctc_dictionary` ADD COLUMN `effective_from` datetime DEFAULT NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dictionary'
                AND COLUMN_NAME = 'effective_to') THEN
    ALTER TABLE `z_ctc_dictionary` ADD COLUMN `effective_to` datetime DEFAULT NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dictionary'
                AND COLUMN_NAME = 'created_by') THEN
    ALTER TABLE `z_ctc_dictionary` ADD COLUMN `created_by` varchar(64) DEFAULT NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_ctc_dictionary'
                AND COLUMN_NAME = 'updated_by') THEN
    ALTER TABLE `z_ctc_dictionary` ADD COLUMN `updated_by` varchar(64) DEFAULT NULL;
  END IF;

  UPDATE `z_ctc_dictionary` SET `status` = 1 WHERE `status` IS NULL;

END$$
DELIMITER ;

CALL `zopc_migrate_dict_flex`();
DROP PROCEDURE IF EXISTS `zopc_migrate_dict_flex`;
