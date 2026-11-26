-- ============================================
-- 4A + SSO 系统数据库初始化脚本
-- 组织管理中心 - Comprehensive-Tissue-Centre
-- ============================================

-- 创建数据库
CREATE
DATABASE IF NOT EXISTS biz_service DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE
biz_service;

-- ============================================
-- 1. 用户表 (sys_user) - 4A账户管理
-- ============================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user
(
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    user_name       VARCHAR(64)  NOT NULL COMMENT '用户名',
    password        VARCHAR(128) NOT NULL COMMENT '密码',
    real_name       VARCHAR(64)  DEFAULT NULL COMMENT '真实姓名',
    email           VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    phone           VARCHAR(32)  DEFAULT NULL COMMENT '手机号',
    status          TINYINT      DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    tenant_id       VARCHAR(64)  DEFAULT NULL COMMENT '租户ID',
    dept_id         BIGINT       DEFAULT NULL COMMENT '部门ID',
    last_login_time DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    gmt_create      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_name (user_name),
    KEY             idx_tenant_id (tenant_id),
    KEY             idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 2. 角色表 (sys_role) - 4A授权管理
-- ============================================
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role
(
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_code    VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name    VARCHAR(64) NOT NULL COMMENT '角色名称',
    description  VARCHAR(256) DEFAULT NULL COMMENT '角色描述',
    status       TINYINT      DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    tenant_id    VARCHAR(64)  DEFAULT NULL COMMENT '租户ID',
    gmt_create   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code),
    KEY          idx_tenant_id (tenant_id),
    KEY          idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ============================================
-- 3. 权限表 (sys_permission) - 4A授权管理
-- ============================================
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission
(
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    perm_code    VARCHAR(128) NOT NULL COMMENT '权限编码',
    perm_name    VARCHAR(64)  NOT NULL COMMENT '权限名称',
    perm_type    VARCHAR(32)  NOT NULL COMMENT '权限类型：MENU-菜单，BUTTON-按钮，API-接口',
    parent_id    BIGINT       DEFAULT 0 COMMENT '父权限ID',
    path         VARCHAR(256) DEFAULT NULL COMMENT '权限路径',
    sort_order   INT          DEFAULT 0 COMMENT '排序号',
    status       TINYINT      DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    tenant_id    VARCHAR(64)  DEFAULT NULL COMMENT '租户ID',
    gmt_create   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_perm_code (perm_code),
    KEY          idx_parent_id (parent_id),
    KEY          idx_tenant_id (tenant_id),
    KEY          idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- ============================================
-- 4. 用户角色关联表 (sys_user_role)
-- ============================================
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role
(
    id         BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    user_id    BIGINT NOT NULL COMMENT '用户ID',
    role_id    BIGINT NOT NULL COMMENT '角色ID',
    gmt_create DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY        idx_user_id (user_id),
    KEY        idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ============================================
-- 5. 角色权限关联表 (sys_role_permission)
-- ============================================
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission
(
    id            BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    role_id       BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    gmt_create    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY           idx_role_id (role_id),
    KEY           idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ============================================
-- 6. 审计日志表 (sys_audit_log) - 4A审计模块
-- ============================================
DROP TABLE IF EXISTS sys_audit_log;
CREATE TABLE sys_audit_log
(
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型：LOGIN/LOGOUT/CREATE/UPDATE/DELETE',
    operation_desc VARCHAR(256) DEFAULT NULL COMMENT '操作描述',
    user_id        BIGINT       DEFAULT NULL COMMENT '用户ID',
    user_name      VARCHAR(64)  DEFAULT NULL COMMENT '用户名',
    tenant_id      VARCHAR(64)  DEFAULT NULL COMMENT '租户ID',
    ip_address     VARCHAR(64)  DEFAULT NULL COMMENT 'IP地址',
    user_agent     VARCHAR(512) DEFAULT NULL COMMENT '用户代理',
    request_url    VARCHAR(512) DEFAULT NULL COMMENT '请求URL',
    request_method VARCHAR(32)  DEFAULT NULL COMMENT '请求方法',
    request_params TEXT         DEFAULT NULL COMMENT '请求参数',
    response_data  TEXT         DEFAULT NULL COMMENT '响应数据',
    execution_time INT          DEFAULT 0 COMMENT '执行时间（毫秒）',
    status         TINYINT      DEFAULT 1 COMMENT '状态：0-失败，1-成功',
    error_msg      TEXT         DEFAULT NULL COMMENT '错误信息',
    gmt_create     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY            idx_user_id (user_id),
    KEY            idx_tenant_id (tenant_id),
    KEY            idx_operation_type (operation_type),
    KEY            idx_gmt_create (gmt_create)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- ============================================
-- 7. 登录日志表 (sys_login_log) - 4A认证模块
-- ============================================
DROP TABLE IF EXISTS sys_login_log;
CREATE TABLE sys_login_log
(
    id         BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    user_id    BIGINT       DEFAULT NULL COMMENT '用户ID',
    user_name  VARCHAR(64)  DEFAULT NULL COMMENT '用户名',
    tenant_id  VARCHAR(64)  DEFAULT NULL COMMENT '租户ID',
    login_type VARCHAR(32)  DEFAULT 'PASSWORD' COMMENT '登录类型：PASSWORD/SMS/QR_CODE',
    ip_address VARCHAR(64)  DEFAULT NULL COMMENT 'IP地址',
    user_agent VARCHAR(512) DEFAULT NULL COMMENT '用户代理',
    browser    VARCHAR(128) DEFAULT NULL COMMENT '浏览器',
    os         VARCHAR(128) DEFAULT NULL COMMENT '操作系统',
    status     TINYINT      DEFAULT 1 COMMENT '状态：0-失败，1-成功',
    error_msg  VARCHAR(512) DEFAULT NULL COMMENT '错误信息',
    gmt_create DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY        idx_user_id (user_id),
    KEY        idx_tenant_id (tenant_id),
    KEY        idx_gmt_create (gmt_create)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- ============================================
-- 初始化数据
-- ============================================

-- 初始化超级管理员角色
INSERT INTO sys_role (role_code, role_name, description, status)
VALUES ('SUPER_ADMIN', '超级管理员', '系统超级管理员，拥有所有权限', 1),
       ('ADMIN', '管理员', '系统管理员', 1),
       ('USER', '普通用户', '普通用户角色', 1);

-- 初始化常用权限
INSERT INTO sys_permission (perm_code, perm_name, perm_type, parent_id, path, sort_order, status)
VALUES ('system', '系统管理', 'MENU', 0, '/system', 1, 1),
       ('system:user', '用户管理', 'MENU', 1, '/system/user', 1, 1),
       ('system:role', '角色管理', 'MENU', 1, '/system/role', 2, 1),
       ('system:permission', '权限管理', 'MENU', 1, '/system/permission', 3, 1),
       ('system:audit', '审计日志', 'MENU', 1, '/system/audit', 4, 1);

-- 初始化超级管理员账号（密码需要加密后存储，这里是明文示例）
-- 实际使用时请使用BCrypt加密：new BCryptPasswordEncoder().encode("admin123")
INSERT INTO sys_user (user_name, password, real_name, email, phone, status)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '超级管理员', 'admin@example.com',
        '13800138000', 1);

-- 为超级管理员分配角色
INSERT INTO sys_user_role (user_id, role_id)
VALUES (1, 1);

-- ============================================
-- TASK001 新增：验证码表 (sys_verify_code)
-- ============================================
DROP TABLE IF EXISTS sys_verify_code;
CREATE TABLE sys_verify_code
(
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    biz_type     VARCHAR(32)  NOT NULL COMMENT '业务类型: REGISTER-注册, LOGIN-登录, RESET_PWD-重置密码',
    code_type    VARCHAR(32)  NOT NULL COMMENT '验证码类型: PHONE-手机, EMAIL-邮箱',
    receiver     VARCHAR(128) NOT NULL COMMENT '接收者(手机号/邮箱)',
    code         VARCHAR(16)  NOT NULL COMMENT '验证码',
    expire_time  DATETIME     NOT NULL COMMENT '过期时间',
    used         TINYINT      NOT NULL DEFAULT '0' COMMENT '是否已使用: 0-否, 1-是',
    gmt_create   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY          idx_receiver_expire (receiver, expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='验证码记录表';
