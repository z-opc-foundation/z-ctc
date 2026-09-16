-- ========================================================
-- 4A + SSO 统一身份认证平台 - 数据库初始化脚本
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- 排序规则: utf8mb4_unicode_ci
-- ========================================================

-- 创建数据库
CREATE
DATABASE IF NOT EXISTS ctc DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE
ctc;

-- ========================================================
-- 1. 租户表 (tenant)
-- 使用 tenant_code 作为主键，不使用自增ID
-- ========================================================
DROP TABLE IF EXISTS tenant;
CREATE TABLE tenant
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
-- 2. 用户表 (user)
-- ========================================================
DROP TABLE IF EXISTS user;
CREATE TABLE user
(
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_name       VARCHAR(50)  NOT NULL COMMENT '用户名',
    password        VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name       VARCHAR(50)           DEFAULT NULL COMMENT '真实姓名',
    email           VARCHAR(100)          DEFAULT NULL COMMENT '邮箱',
    phone           VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    avatar          VARCHAR(255)          DEFAULT NULL COMMENT '头像URL',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
    tenant_code     VARCHAR(64)  NOT NULL COMMENT '租户编码',
    dept_id         BIGINT                DEFAULT NULL COMMENT '部门ID',
    last_login_time DATETIME              DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip   VARCHAR(50)           DEFAULT NULL COMMENT '最后登录IP',
    gmt_create      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_name (user_name),
    KEY             idx_tenant_code (tenant_code),
    KEY             idx_dept_id (dept_id),
    KEY             idx_status (status),
    KEY             idx_gmt_create (gmt_create)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ========================================================
-- 3. 角色表 (role)
-- ========================================================
DROP TABLE IF EXISTS role;
CREATE TABLE role
(
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_code    VARCHAR(50) NOT NULL COMMENT '角色编码',
    role_name    VARCHAR(50) NOT NULL COMMENT '角色名称',
    description  VARCHAR(255)         DEFAULT NULL COMMENT '角色描述',
    status       TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
    tenant_code  VARCHAR(64) NOT NULL COMMENT '租户编码',
    gmt_create   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted   TINYINT     NOT NULL DEFAULT 0 COMMENT '是否删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code),
    KEY          idx_tenant_code (tenant_code),
    KEY          idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ========================================================
-- 4. 权限表 (permission)
-- ========================================================
DROP TABLE IF EXISTS permission;
CREATE TABLE permission
(
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    perm_code    VARCHAR(100) NOT NULL COMMENT '权限编码（如：user:create, /api/users）',
    perm_name    VARCHAR(50)  NOT NULL COMMENT '权限名称',
    perm_type    VARCHAR(20)  NOT NULL COMMENT '权限类型：MENU-菜单，BUTTON-按钮，API-API接口',
    parent_id    BIGINT                DEFAULT 0 COMMENT '父权限ID，0表示顶级',
    path         VARCHAR(255)          DEFAULT NULL COMMENT '路径（菜单或API路径）',
    sort_order   INT                   DEFAULT 0 COMMENT '排序顺序',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
    tenant_code  VARCHAR(64)  NOT NULL COMMENT '租户编码',
    gmt_create   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除 0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_perm_code (perm_code),
    KEY          idx_parent_id (parent_id),
    KEY          idx_tenant_code (tenant_code),
    KEY          idx_status (status),
    KEY          idx_perm_type (perm_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- ========================================================
-- 5. 用户角色关联表 (user_role)
-- ========================================================
DROP TABLE IF EXISTS user_role;
CREATE TABLE user_role
(
    id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id    BIGINT   NOT NULL COMMENT '用户ID',
    role_id    BIGINT   NOT NULL COMMENT '角色ID',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY        idx_user_id (user_id),
    KEY        idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ========================================================
-- 6. 角色权限关联表 (role_permission)
-- ========================================================
DROP TABLE IF EXISTS role_permission;
CREATE TABLE role_permission
(
    id            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_id       BIGINT   NOT NULL COMMENT '角色ID',
    permission_id BIGINT   NOT NULL COMMENT '权限ID',
    gmt_create    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY           idx_role_id (role_id),
    KEY           idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ========================================================
-- 7. 审计日志表 (audit_log)
-- ========================================================
DROP TABLE IF EXISTS audit_log;
CREATE TABLE audit_log
(
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    operator_id    BIGINT                DEFAULT NULL COMMENT '操作人ID',
    operator_name  VARCHAR(50)           DEFAULT NULL COMMENT '操作人名称',
    operation_type VARCHAR(20)  NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/LOGIN/LOGOUT/EXPORT/IMPORT',
    target_type    VARCHAR(50)  NOT NULL COMMENT '操作对象类型：USER/ROLE/PERMISSION/TENANT',
    target_id      VARCHAR(100) NOT NULL COMMENT '操作对象ID',
    description    VARCHAR(500)          DEFAULT NULL COMMENT '操作描述',
    before_value   TEXT                  DEFAULT NULL COMMENT '变更前数据（JSON）',
    after_value    TEXT                  DEFAULT NULL COMMENT '变更后数据（JSON）',
    ip_address     VARCHAR(50)           DEFAULT NULL COMMENT 'IP地址',
    user_agent     VARCHAR(500)          DEFAULT NULL COMMENT '用户代理',
    request_url    VARCHAR(500)          DEFAULT NULL COMMENT '请求URL',
    request_method VARCHAR(10)           DEFAULT NULL COMMENT '请求方法：GET/POST/PUT/DELETE',
    gmt_create     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY            idx_operator_id (operator_id),
    KEY            idx_operation_type (operation_type),
    KEY            idx_target_type (target_type),
    KEY            idx_target_id (target_id),
    KEY            idx_gmt_create (gmt_create)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- ========================================================
-- 初始化数据
-- ========================================================

-- 插入默认租户
INSERT INTO tenant (tenant_code, tenant_name, contact_name, contact_phone, contact_email, status, expire_time)
VALUES ('default', '默认租户', '系统管理员', '13800138000', 'admin@example.com', 1, '2099-12-31 23:59:59');

-- 插入默认用户（密码：123456，BCrypt加密）
-- 密码加密后的值：$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO
INSERT INTO user (user_name, password, real_name, email, phone, status, tenant_code, last_login_time, last_login_ip)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '系统管理员', 'admin@example.com',
        '13800138000', 1, 'default', NOW(), '127.0.0.1');

-- 插入默认角色
INSERT INTO role (role_code, role_name, description, status, tenant_code)
VALUES ('super_admin', '超级管理员', '拥有所有权限', 1, 'default');

INSERT INTO role (role_code, role_name, description, status, tenant_code)
VALUES ('admin', '管理员', '拥有大部分管理权限', 1, 'default');

INSERT INTO role (role_code, role_name, description, status, tenant_code)
VALUES ('user', '普通用户', '仅拥有基本权限', 1, 'default');

-- 关联超级管理员角色
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM user u,
     role r
WHERE u.user_name = 'admin'
  AND r.role_code = 'super_admin';
