-- =============================================================================
-- z_ctc_ac_dept 增加 parent_code 列（部门层级）
-- =============================================================================
-- 背景（2026-10-04 实测）:
--   1. 消费方 z-opc 的 TeamStaffAdapter.fetchOrgTree 需要**带人员的嵌套部门树**,
--      调用 POST /ctc-osc/dept/getStaffDeptTree —— 该端点本仓从未定义。
--   2. 本仓 z_ctc_ac_dept 表**没有父级列**, DeptDO 也就无从表达层级,
--      导致即便补了树接口也只能退化成平铺列表。
--   3. 对照证据: 并列的旧表 z_ctc_dept **有** parent_code,
--      且 _doc/002_deploy/init/code-based-migration.sql 曾把 z_ctc_dept 的层级
--      迁成 dept_code + parent_code 三元组 —— 但 ac 体系(新表)没跟上。
--
-- 本脚本只加列，不动既有数据；可重复执行（幂等）。
--
-- ⚠️ 执行前请确认: 生产环境 z_ctc_ac_dept 若已有数据，加列后 parent_code
--    默认为 NULL，即所有部门都是根节点 —— 这是**安全降级**（不会出现环或丢数据）。
--    层级需由业务侧按需 UPDATE 回填。

-- 1) 加列（幂等: 仅在列不存在时执行）
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'z_ctc_ac_dept'
      AND COLUMN_NAME  = 'parent_code'
);
SET @ddl = IF(@col_exists > 0,
    'SELECT ''z_ctc_ac_dept.parent_code 已存在, 跳过'' AS msg',
    'ALTER TABLE z_ctc_ac_dept ADD COLUMN `parent_code` VARCHAR(64) NULL COMMENT ''父部门编码; 根部门为 NULL''');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 建索引：组树按 parent_code 挂接，且 (tenant, domain) 为主要过滤条件
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'z_ctc_ac_dept'
      AND INDEX_NAME   = 'idx_dept_parent'
);
SET @ddl2 = IF(@idx_exists > 0,
    'SELECT ''idx_dept_parent 已存在, 跳过'' AS msg',
    'ALTER TABLE z_ctc_ac_dept ADD INDEX `idx_dept_parent` (`tenant_code`, `domain_code`, `parent_code`)');
PREPARE stmt2 FROM @ddl2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 3) 自引用完整性检查（只报告, 不修改）:
--    parent_code 应指向同 (tenant_code, domain_code) 下存在的 dept_code。
--    下面这条应当返回 0 行; 若非 0, 需人工修正后再组树。
--
-- SELECT d.dept_code, d.parent_code, d.tenant_code, d.domain_code
-- FROM z_ctc_ac_dept d
-- LEFT JOIN z_ctc_ac_dept p
--   ON p.tenant_code = d.tenant_code
--  AND p.domain_code = d.domain_code
--  AND p.dept_code   = d.parent_code
-- WHERE d.parent_code IS NOT NULL
--   AND d.parent_code <> ''
--   AND p.dept_code IS NULL;

-- 4) 环检查（只报告, 不修改）:
--    自环: parent_code = dept_code。下面这条应当返回 0 行。
--
-- SELECT dept_code FROM z_ctc_ac_dept
-- WHERE parent_code IS NOT NULL AND parent_code = dept_code;
