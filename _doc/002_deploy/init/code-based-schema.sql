-- ========================================================
-- CTC 模块表结构 - Code-Based 关联规范
-- 数据库: oc
-- 字符集: utf8mb4
-- ========================================================
-- 关联规范：
--   所有层级关联强制使用 code，不使用 id
--   层级：tenant -> domain -> org -> dept -> group
--   user/dept/dept 表通过 dept_code 关联
--   组不设上级 parent_id（扁平结构，按租户+部门隔离）
-- ========================================================

-- ========================================================
-- 1. 租户表 (z_ctc_tenant)
-- 主键：tenant_code
-- ========================================================
DROP TABLE IF EXISTS z_ctc_tenant;
CREATE TABLE z_ctc_tenant
(
    tenant_code   VARCHAR(64)  NOT NULL COMMENT '租户编码（主键）',
    tenant_name   VARCHAR(100) NOT NULL COMMENT '租户名称',
    contact_name  VARCHAR(50)           DEFAULT NULL COMMENT '联系人',
    contact_phone VARCHAR(20)           DEFAULT NULL COMMENT '联系电话',
    contact_email VARCHAR(100)          DEFAULT NULL COMMENT '联系邮箱',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
    expire_time   DATETIME              DEFAULT NULL COMMENT '过期时间',
    gmt_create    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除 0-未删除 1-已删除',
    PRIMARY KEY (tenant_code),
    UNIQUE KEY uk_tenant_name (tenant_name),
    KEY           idx_status (status),
    KEY           idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表';

-- ========================================================
-- 2. 域表 (z_ctc_domain)
-- 主键：domain_code
-- 外键：tenant_code
-- 关联：domain -> tenant（通过 tenant_code）
-- ========================================================
DROP TABLE IF EXISTS z_ctc_domain;
CREATE TABLE z_ctc_domain
(
    domain_code  VARCHAR(64)  NOT NULL COMMENT '域编码（主键）',
    domain_name  VARCHAR(100) NOT NULL COMMENT '域名称',
    tenant_code  VARCHAR(64)  NOT NULL COMMENT '租户编码',
    status       TINYINT      DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
    description  VARCHAR(500) DEFAULT NULL COMMENT '描述',
    gmt_create   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (domain_code),
    KEY          idx_tenant_code (tenant_code),
    KEY          idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='域表';

-- ========================================================
-- 3. 组织表 (z_ctc_org)
-- 主键：org_code
-- 外键：tenant_code, domain_code
-- 关联：org -> tenant（tenant_code）/ domain（domain_code）
-- ========================================================
DROP TABLE IF EXISTS z_ctc_org;
CREATE TABLE z_ctc_org
(
    org_code     VARCHAR(64)  NOT NULL COMMENT '组织编码（主键）',
    org_name     VARCHAR(100) NOT NULL COMMENT '组织名称',
    tenant_code  VARCHAR(64)  NOT NULL COMMENT '租户编码',
    domain_code  VARCHAR(64)  NOT NULL COMMENT '域编码',
    parent_code  VARCHAR(64)  DEFAULT NULL COMMENT '上级组织编码（NULL 表示顶级）',
    status       TINYINT      DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
    description  VARCHAR(500) DEFAULT NULL COMMENT '描述',
    gmt_create   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (org_code),
    KEY          idx_tenant_code (tenant_code),
    KEY          idx_domain_code (domain_code),
    KEY          idx_parent_code (parent_code),
    KEY          idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织表';

-- ========================================================
-- 4. 部门表 (z_ctc_dept)
-- 主键：dept_code
-- 外键：tenant_code, domain_code, org_code
-- 关联：dept -> tenant（tenant_code）/ domain（domain_code）/ org（org_code）
-- ========================================================
DROP TABLE IF EXISTS z_ctc_dept;
CREATE TABLE z_ctc_dept
(
    dept_code    VARCHAR(64)  NOT NULL COMMENT '部门编码（主键）',
    dept_name    VARCHAR(100) NOT NULL COMMENT '部门名称',
    tenant_code  VARCHAR(64)  NOT NULL COMMENT '租户编码',
    domain_code  VARCHAR(64)  NOT NULL COMMENT '域编码',
    org_code     VARCHAR(64)  NOT NULL COMMENT '组织编码',
    parent_code  VARCHAR(64)  DEFAULT NULL COMMENT '上级部门编码（NULL 表示顶级）',
    status       TINYINT      DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
    description  VARCHAR(500) DEFAULT NULL COMMENT '描述',
    gmt_create   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (dept_code),
    KEY          idx_tenant_code (tenant_code),
    KEY          idx_domain_code (domain_code),
    KEY          idx_org_code (org_code),
    KEY          idx_parent_code (parent_code),
    KEY          idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- ========================================================
-- 5. 组表 (z_ctc_group)
-- 主键：group_code
-- 外键：tenant_code, domain_code, org_code, dept_code
-- 关联：group -> tenant（tenant_code）/ domain（domain_code）/ org（org_code）/ dept（dept_code）
-- 注意：组扁平化，不设 parent_code
-- ========================================================
DROP TABLE IF EXISTS z_ctc_group;
CREATE TABLE z_ctc_group
(
    group_code   VARCHAR(64)  NOT NULL COMMENT '组编码（主键）',
    group_name   VARCHAR(100) NOT NULL COMMENT '组名称',
    tenant_code  VARCHAR(64)  NOT NULL COMMENT '租户编码',
    domain_code  VARCHAR(64)  NOT NULL COMMENT '域编码',
    org_code     VARCHAR(64)  NOT NULL COMMENT '组织编码',
    dept_code    VARCHAR(64)  NOT NULL COMMENT '部门编码',
    status       TINYINT      DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
    description  VARCHAR(500) DEFAULT NULL COMMENT '描述',
    gmt_create   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (group_code),
    KEY          idx_tenant_code (tenant_code),
    KEY          idx_domain_code (domain_code),
    KEY          idx_org_code (org_code),
    KEY          idx_dept_code (dept_code),
    KEY          idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组表';

-- ========================================================
-- 6. 用户表 (z_ctc_user)
-- 外键：tenant_code, dept_code
-- 关联：user -> tenant（tenant_code）/ dept（dept_code）
-- 注意：role 仍用 role_id（角色表为全局 ID，不按租户隔离）
-- ========================================================
DROP TABLE IF EXISTS z_ctc_user;
CREATE TABLE z_ctc_user
(
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    user_name       VARCHAR(64)  NOT NULL COMMENT '用户名',
    password        VARCHAR(128) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name       VARCHAR(64)           DEFAULT NULL COMMENT '真实姓名',
    email           VARCHAR(128)          DEFAULT NULL COMMENT '邮箱',
    phone           VARCHAR(32)           DEFAULT NULL COMMENT '手机号',
    status          TINYINT               DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
    tenant_code     VARCHAR(64)  NOT NULL COMMENT '租户编码',
    dept_code       VARCHAR(64)           DEFAULT NULL COMMENT '部门编码',
    last_login_time DATETIME              DEFAULT NULL COMMENT '最后登录时间',
    gmt_create      DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified    DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_name (user_name),
    KEY             idx_tenant_code (tenant_code),
    KEY             idx_dept_code (dept_code),
    KEY             idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ========================================================
-- 7. 角色表 (z_ctc_role)
-- 主键：id（全局自增，不做 code）
-- 外键：tenant_code
-- 说明：角色 ID 全局共享，不需要按租户区分 code
-- ========================================================
DROP TABLE IF EXISTS z_ctc_role;
CREATE TABLE z_ctc_role
(
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_code    VARCHAR(64) NOT NULL COMMENT '角色编码（唯一）',
    role_name    VARCHAR(64) NOT NULL COMMENT '角色名称',
    description  VARCHAR(256)         DEFAULT NULL COMMENT '角色描述',
    status       TINYINT              DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
    tenant_code  VARCHAR(64)          DEFAULT NULL COMMENT '租户编码（NULL=全局角色）',
    gmt_create   DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted   TINYINT     NOT NULL DEFAULT 0 COMMENT '是否删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code),
    KEY          idx_tenant_code (tenant_code),
    KEY          idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ========================================================
-- 8. 权限表 (z_ctc_permission)
-- 主键：id
-- 外键：tenant_code
-- 说明：parent_id 保留（树形结构，id 不改）
-- ========================================================
DROP TABLE IF EXISTS z_ctc_permission;
CREATE TABLE z_ctc_permission
(
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    perm_code    VARCHAR(128) NOT NULL COMMENT '权限编码',
    perm_name    VARCHAR(64)  NOT NULL COMMENT '权限名称',
    perm_type    VARCHAR(32)  NOT NULL COMMENT '权限类型：MENU/BUTTON/API',
    parent_id    BIGINT                DEFAULT 0 COMMENT '父权限ID（树形保留）',
    path         VARCHAR(256)          DEFAULT NULL COMMENT '路径',
    sort_order   INT                   DEFAULT 0 COMMENT '排序号',
    status       TINYINT               DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
    tenant_code  VARCHAR(64)           DEFAULT NULL COMMENT '租户编码',
    gmt_create   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_perm_code (perm_code),
    KEY          idx_parent_id (parent_id),
    KEY          idx_tenant_code (tenant_code),
    KEY          idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- ========================================================
-- 9. 用户角色关联表 (z_ctc_user_role)
-- 关联：user_id, role_id（两者都是 id，不改）
-- ========================================================
DROP TABLE IF EXISTS z_ctc_user_role;
CREATE TABLE z_ctc_user_role
(
    id         BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    user_id    BIGINT NOT NULL COMMENT '用户ID',
    role_id    BIGINT NOT NULL COMMENT '角色ID',
    gmt_create DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY        idx_user_id (user_id),
    KEY        idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ========================================================
-- 10. 角色权限关联表 (z_ctc_role_permission)
-- 关联：role_id, permission_id（两者都是 id，不改）
-- ========================================================
DROP TABLE IF EXISTS z_ctc_role_permission;
CREATE TABLE z_ctc_role_permission
(
    id            BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    role_id       BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    gmt_create    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY           idx_role_id (role_id),
    KEY           idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ========================================================
-- 11. 审计日志表 (z_ctc_audit_log) - 不变
-- ========================================================
DROP TABLE IF EXISTS z_ctc_audit_log;
CREATE TABLE z_ctc_audit_log
(
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    operation_desc VARCHAR(256) DEFAULT NULL COMMENT '操作描述',
    user_id        BIGINT       DEFAULT NULL COMMENT '用户ID',
    user_name      VARCHAR(64)  DEFAULT NULL COMMENT '用户名',
    tenant_code    VARCHAR(64)  DEFAULT NULL COMMENT '租户编码',
    ip_address     VARCHAR(64)  DEFAULT NULL COMMENT 'IP地址',
    user_agent     VARCHAR(512) DEFAULT NULL COMMENT '用户代理',
    request_url    VARCHAR(512) DEFAULT NULL COMMENT '请求URL',
    request_method VARCHAR(32)  DEFAULT NULL COMMENT '请求方法',
    request_params TEXT         DEFAULT NULL COMMENT '请求参数',
    response_data  TEXT         DEFAULT NULL COMMENT '响应数据',
    execution_time INT          DEFAULT 0 COMMENT '执行时间（毫秒）',
    status         TINYINT      DEFAULT 1 COMMENT '状态 0-失败 1-成功',
    error_msg      TEXT         DEFAULT NULL COMMENT '错误信息',
    gmt_create     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY            idx_user_id (user_id),
    KEY            idx_tenant_code (tenant_code),
    KEY            idx_operation_type (operation_type),
    KEY            idx_gmt_create (gmt_create)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- ========================================================
-- 12. 登录日志表 (z_ctc_login_log) - 不变
-- ========================================================
DROP TABLE IF EXISTS z_ctc_login_log;
CREATE TABLE z_ctc_login_log
(
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    user_id     BIGINT       DEFAULT NULL COMMENT '用户ID',
    user_name   VARCHAR(64)  DEFAULT NULL COMMENT '用户名',
    tenant_code VARCHAR(64)  DEFAULT NULL COMMENT '租户编码',
    login_type  VARCHAR(32)  DEFAULT 'PASSWORD' COMMENT '登录类型',
    ip_address  VARCHAR(64)  DEFAULT NULL COMMENT 'IP地址',
    user_agent  VARCHAR(512) DEFAULT NULL COMMENT '用户代理',
    browser     VARCHAR(128) DEFAULT NULL COMMENT '浏览器',
    os          VARCHAR(128) DEFAULT NULL COMMENT '操作系统',
    status      TINYINT      DEFAULT 1 COMMENT '状态 0-失败 1-成功',
    error_msg   VARCHAR(512) DEFAULT NULL COMMENT '错误信息',
    gmt_create  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY         idx_user_id (user_id),
    KEY         idx_tenant_code (tenant_code),
    KEY         idx_gmt_create (gmt_create)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- ========================================================
-- 13. 验证码表 (z_ctc_verify_code) - 不变
-- ========================================================
DROP TABLE IF EXISTS z_ctc_verify_code;
CREATE TABLE z_ctc_verify_code
(
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    biz_type     VARCHAR(32)  NOT NULL COMMENT '业务类型',
    code_type    VARCHAR(32)  NOT NULL COMMENT '验证码类型',
    receiver     VARCHAR(128) NOT NULL COMMENT '接收者',
    code         VARCHAR(16)  NOT NULL COMMENT '验证码',
    expire_time  DATETIME     NOT NULL COMMENT '过期时间',
    used         TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已使用 0-否 1-是',
    gmt_create   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY          idx_receiver_expire (receiver, expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='验证码记录表';

-- ========================================================
-- 初始化数据
-- ========================================================

-- 租户
INSERT INTO z_ctc_tenant (tenant_code, tenant_name, contact_name, contact_phone, contact_email, status, expire_time)
VALUES ('default', '默认租户', '系统管理员', '13800138000', 'admin@example.com', 1, '2099-12-31 23:59:59');

-- 域（关联 tenant_code）
INSERT INTO z_ctc_domain (domain_code, domain_name, tenant_code, status, description)
VALUES ('default_domain', '默认域', 'default', 1, '默认租户下的默认域');

-- 组织（关联 tenant_code + domain_code）
INSERT INTO z_ctc_org (org_code, org_name, tenant_code, domain_code, parent_code, status, description)
VALUES ('default_org', '默认组织', 'default', 'default_domain', NULL, 1, '默认组织');

-- 部门（关联 tenant_code + domain_code + org_code）
INSERT INTO z_ctc_dept (dept_code, dept_name, tenant_code, domain_code, org_code, parent_code, status, description)
VALUES ('default_dept', '默认部门', 'default', 'default_domain', 'default_org', NULL, 1, '默认部门');

-- 组（关联 tenant_code + domain_code + org_code + dept_code）
INSERT INTO z_ctc_group (group_code, group_name, tenant_code, domain_code, org_code, dept_code, status, description)
VALUES ('default_group', '默认组', 'default', 'default_domain', 'default_org', 'default_dept', 1, '默认组');

-- 用户（关联 tenant_code + dept_code）
INSERT INTO z_ctc_user (user_name, password, real_name, email, phone, status, tenant_code, dept_code, last_login_time)
VALUES ('admin', '$2b$10$BCbPO9gqCY7DleCYH6RlBeJkHuIpm8nEjy4SsWgG7VoR9ZOKHYppm', '超级管理员', 'admin@example.com',
        '13800138000', 1, 'default', 'default_dept', NOW());

-- 角色（全局，tenant_code 为 NULL）
INSERT INTO z_ctc_role (role_code, role_name, description, status, tenant_code)
VALUES ('SUPER_ADMIN', '超级管理员', '系统超级管理员，拥有所有权限', 1, NULL);

INSERT INTO z_ctc_role (role_code, role_name, description, status, tenant_code)
VALUES ('ADMIN', '管理员', '系统管理员', 1, NULL);

INSERT INTO z_ctc_role (role_code, role_name, description, status, tenant_code)
VALUES ('USER', '普通用户', '普通用户角色', 1, NULL);

-- 权限（全局，tenant_code 为 NULL）
INSERT INTO z_ctc_permission (perm_code, perm_name, perm_type, parent_id, path, sort_order, status, tenant_code)
VALUES ('system', '系统管理', 'MENU', 0, '/system', 1, 1, NULL);

INSERT INTO z_ctc_permission (perm_code, perm_name, perm_type, parent_id, path, sort_order, status, tenant_code)
VALUES ('system:user', '用户管理', 'MENU', 1, '/system/user', 1, 1, NULL);

INSERT INTO z_ctc_permission (perm_code, perm_name, perm_type, parent_id, path, sort_order, status, tenant_code)
VALUES ('system:role', '角色管理', 'MENU', 1, '/system/role', 2, 1, NULL);

INSERT INTO z_ctc_permission (perm_code, perm_name, perm_type, parent_id, path, sort_order, status, tenant_code)
VALUES ('system:permission', '权限管理', 'MENU', 1, '/system/permission', 3, 1, NULL);

INSERT INTO z_ctc_permission (perm_code, perm_name, perm_type, parent_id, path, sort_order, status, tenant_code)
VALUES ('system:audit', '审计日志', 'MENU', 1, '/system/audit', 4, 1, NULL);

-- 为 admin 分配 SUPER_ADMIN 角色
INSERT INTO z_ctc_user_role (user_id, role_id)
VALUES (1, 1);
