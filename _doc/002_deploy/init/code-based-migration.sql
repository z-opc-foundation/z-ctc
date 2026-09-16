-- ========================================================
-- CTC Code-Based 关联迁移 DDL（最终版）
-- 线上库 oc
-- 执行顺序：严格按依赖顺序
-- ========================================================

-- ====== 1. z_ctc_tenant ======
-- 现状：id(BIGINT自增 PK) + tenant_code(VARCHAR)
-- 目标：tenant_code VARCHAR PK，去掉 id 列
-- 注意：child 表的 tenant_id FK 在各自迁移步中处理

ALTER TABLE z_ctc_tenant
    ADD COLUMN tenant_code_new VARCHAR(64) NOT NULL DEFAULT '' COMMENT '租户编码（新PK）' AFTER id,
    ADD PRIMARY KEY (tenant_code_new);
UPDATE z_ctc_tenant
SET tenant_code_new = tenant_code
WHERE tenant_code_new = ''
   OR tenant_code_new IS NULL;
ALTER TABLE z_ctc_tenant DROP PRIMARY KEY;
ALTER TABLE z_ctc_tenant CHANGE COLUMN tenant_code_new tenant_code VARCHAR (64) NOT NULL COMMENT '租户编码（PK）',
    ADD PRIMARY KEY (tenant_code);
ALTER TABLE z_ctc_tenant DROP COLUMN id;


-- ====== 2. z_ctc_domain ======
-- 现状：id PK + domain_code + tenant_id(FK→tenant.id)
-- 目标：domain_code PK + tenant_code（替换 tenant_id）

-- Step A: 清除旧 tenant_id 数据（避免 FK 约束干扰），用 NULL 过渡
UPDATE z_ctc_domain
SET tenant_id = NULL
WHERE 1 = 1;

-- Step B: 加新列，迁移数据
ALTER TABLE z_ctc_domain
    ADD COLUMN domain_code_new VARCHAR(64) NOT NULL DEFAULT '' COMMENT '域编码（新PK）' AFTER id,
    ADD COLUMN tenant_code VARCHAR(64) DEFAULT NULL COMMENT '租户编码' AFTER domain_name;
UPDATE z_ctc_domain
SET domain_code_new = domain_code
WHERE domain_code_new = ''
   OR domain_code_new IS NULL;
UPDATE z_ctc_domain d
    JOIN z_ctc_tenant t
ON d.tenant_id <=> t.id
    SET d.tenant_code = t.tenant_code;

-- Step C: 删除旧 PK 和 id 列
ALTER TABLE z_ctc_domain DROP PRIMARY KEY;
ALTER TABLE z_ctc_domain CHANGE COLUMN domain_code_new domain_code VARCHAR (64) NOT NULL COMMENT '域编码（PK）',
    ADD PRIMARY KEY (domain_code);
ALTER TABLE z_ctc_domain DROP COLUMN id;
ALTER TABLE z_ctc_domain DROP COLUMN tenant_id;


-- ====== 3. z_ctc_org ======
-- 现状：id PK + tenant_id(FK) + domain_id(FK) + parent_id
-- 目标：org_code PK + tenant_code + domain_code + parent_code

ALTER TABLE z_ctc_org
    ADD COLUMN org_code_new VARCHAR(64) NOT NULL DEFAULT '' COMMENT '组织编码（新PK）' AFTER id,
    ADD COLUMN tenant_code VARCHAR(64) DEFAULT NULL COMMENT '租户编码' AFTER org_name,
    ADD COLUMN domain_code VARCHAR(64) DEFAULT NULL COMMENT '域编码' AFTER tenant_code,
    ADD COLUMN parent_code VARCHAR(64) DEFAULT NULL COMMENT '上级组织编码' AFTER domain_code;
UPDATE z_ctc_org
SET org_code_new = LOWER(REPLACE(org_name, ' ', '_'))
WHERE org_code_new = ''
   OR org_code_new IS NULL;
UPDATE z_ctc_org o
    JOIN z_ctc_tenant t
ON o.tenant_id <=> t.id
    SET o.tenant_code = t.tenant_code;
UPDATE z_ctc_org o
    JOIN z_ctc_domain d
ON o.domain_id <=> d.id
    SET o.domain_code = d.domain_code;
UPDATE z_ctc_org o
    JOIN z_ctc_org p
ON o.parent_id <=> p.id
    SET o.parent_code = LOWER (REPLACE(p.org_name, ' ', '_'));
ALTER TABLE z_ctc_org DROP PRIMARY KEY;
ALTER TABLE z_ctc_org CHANGE COLUMN org_code_new org_code VARCHAR (64) NOT NULL COMMENT '组织编码（PK）',
    ADD PRIMARY KEY (org_code);
ALTER TABLE z_ctc_org DROP COLUMN id;
ALTER TABLE z_ctc_org DROP COLUMN tenant_id;
ALTER TABLE z_ctc_org DROP COLUMN domain_id;
ALTER TABLE z_ctc_org DROP COLUMN parent_id;


-- ====== 4. z_ctc_dept ======
-- 现状：id PK + tenant_id + domain_id + org_id + parent_id
-- 目标：dept_code PK + tenant_code + domain_code + org_code + parent_code

ALTER TABLE z_ctc_dept
    ADD COLUMN dept_code_new VARCHAR(64) NOT NULL DEFAULT '' COMMENT '部门编码（新PK）' AFTER id,
    ADD COLUMN tenant_code VARCHAR(64) DEFAULT NULL COMMENT '租户编码' AFTER dept_name,
    ADD COLUMN domain_code VARCHAR(64) DEFAULT NULL COMMENT '域编码' AFTER tenant_code,
    ADD COLUMN org_code VARCHAR(64) DEFAULT NULL COMMENT '组织编码' AFTER domain_code,
    ADD COLUMN parent_code VARCHAR(64) DEFAULT NULL COMMENT '上级部门编码' AFTER org_code;
UPDATE z_ctc_dept
SET dept_code_new = LOWER(REPLACE(dept_name, ' ', '_'))
WHERE dept_code_new = ''
   OR dept_code_new IS NULL;
UPDATE z_ctc_dept d
    JOIN z_ctc_tenant t
ON d.tenant_id <=> t.id
    SET d.tenant_code = t.tenant_code;
UPDATE z_ctc_dept d
    JOIN z_ctc_domain dom
ON d.domain_id <=> dom.id
    SET d.domain_code = dom.domain_code;
UPDATE z_ctc_dept d
    JOIN z_ctc_org o
ON d.org_id <=> o.id
    SET d.org_code = LOWER (REPLACE(o.org_name, ' ', '_'));
UPDATE z_ctc_dept d
    JOIN z_ctc_dept p
ON d.parent_id <=> p.id
    SET d.parent_code = LOWER (REPLACE(p.dept_name, ' ', '_'));
ALTER TABLE z_ctc_dept DROP PRIMARY KEY;
ALTER TABLE z_ctc_dept CHANGE COLUMN dept_code_new dept_code VARCHAR (64) NOT NULL COMMENT '部门编码（PK）',
    ADD PRIMARY KEY (dept_code);
ALTER TABLE z_ctc_dept DROP COLUMN id;
ALTER TABLE z_ctc_dept DROP COLUMN tenant_id;
ALTER TABLE z_ctc_dept DROP COLUMN domain_id;
ALTER TABLE z_ctc_dept DROP COLUMN org_id;
ALTER TABLE z_ctc_dept DROP COLUMN parent_id;


-- ====== 5. z_ctc_group ======
-- 现状：id PK + tenant_id + domain_id + org_id + dept_id
-- 目标：group_code PK + tenant_code + domain_code + org_code + dept_code

ALTER TABLE z_ctc_group
    ADD COLUMN group_code_new VARCHAR(64) NOT NULL DEFAULT '' COMMENT '组编码（新PK）' AFTER id,
    ADD COLUMN tenant_code VARCHAR(64) DEFAULT NULL COMMENT '租户编码' AFTER group_name,
    ADD COLUMN domain_code VARCHAR(64) DEFAULT NULL COMMENT '域编码' AFTER tenant_code,
    ADD COLUMN org_code VARCHAR(64) DEFAULT NULL COMMENT '组织编码' AFTER domain_code,
    ADD COLUMN dept_code VARCHAR(64) DEFAULT NULL COMMENT '部门编码' AFTER org_code;
UPDATE z_ctc_group
SET group_code_new = LOWER(REPLACE(group_name, ' ', '_'))
WHERE group_code_new = ''
   OR group_code_new IS NULL;
UPDATE z_ctc_group g
    JOIN z_ctc_tenant t
ON g.tenant_id <=> t.id
    SET g.tenant_code = t.tenant_code;
UPDATE z_ctc_group g
    JOIN z_ctc_domain d
ON g.domain_id <=> d.id
    SET g.domain_code = d.domain_code;
UPDATE z_ctc_group g
    JOIN z_ctc_org o
ON g.org_id <=> o.id
    SET g.org_code = LOWER (REPLACE(o.org_name, ' ', '_'));
UPDATE z_ctc_group g
    JOIN z_ctc_dept d
ON g.dept_id <=> d.id
    SET g.dept_code = LOWER (REPLACE(d.dept_name, ' ', '_'));
ALTER TABLE z_ctc_group DROP PRIMARY KEY;
ALTER TABLE z_ctc_group CHANGE COLUMN group_code_new group_code VARCHAR (64) NOT NULL COMMENT '组编码（PK）',
    ADD PRIMARY KEY (group_code);
ALTER TABLE z_ctc_group DROP COLUMN id;
ALTER TABLE z_ctc_group DROP COLUMN tenant_id;
ALTER TABLE z_ctc_group DROP COLUMN domain_id;
ALTER TABLE z_ctc_group DROP COLUMN org_id;
ALTER TABLE z_ctc_group DROP COLUMN dept_id;


-- ====== 6. z_ctc_user ======
-- 现状：dept_id(BIGINT) 可NULL
-- 目标：dept_code(VARCHAR) 可NULL

ALTER TABLE z_ctc_user
    ADD COLUMN dept_code VARCHAR(64) DEFAULT NULL COMMENT '部门编码' AFTER tenant_code;
UPDATE z_ctc_user u
    JOIN z_ctc_dept d
ON u.dept_id <=> d.id
    SET u.dept_code = d.dept_code;
ALTER TABLE z_ctc_user DROP COLUMN dept_id;
