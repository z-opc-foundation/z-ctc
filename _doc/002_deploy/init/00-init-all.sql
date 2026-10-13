-- z-ctc 4A 认证中心初始化脚本
-- 由 deploy/gen-init-sql.py 从 4 个源文件装配而来, 请勿手改
CREATE DATABASE IF NOT EXISTS z_ctc DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE z_ctc;

-- ==== 1. ac 基础表 ====

-- =============================================================================
-- z-ctc-ac 账号管理域 SQL (MySQL 8.0 + Druid 1.2.18)
--
-- 7 张表: 账号 / 凭证 / 登录日志 / 租户 / 组织 / 部门 / 组别.
-- z-opc 范围内统一前缀 z_ctc_* 风格.
-- =============================================================================

-- 账号主表
CREATE TABLE IF NOT EXISTS `z_ctc_ac_account`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `account_no`
    VARCHAR
(
    64
) NOT NULL COMMENT '账号编号 (业务可读唯一标识)',
    `username` VARCHAR
(
    64
) NOT NULL COMMENT '用户名 (登录名)',
    `nickname` VARCHAR
(
    64
) NULL COMMENT '昵称',
    `password_hash` VARCHAR
(
    256
) NULL COMMENT 'BCrypt 密码哈希 (账号类型=密码登录时存)',
    `password_salt` VARCHAR
(
    64
) NULL COMMENT '密码盐值 (历史兼容, BCrypt 自带盐, 一般不用)',
    `account_type` TINYINT NOT NULL DEFAULT 3 COMMENT '账号类型: 1=超级管理员 2=租户管理员 3=普通成员 4=临时账号',
    `tenant_code` VARCHAR
(
    64
) NULL COMMENT '租户 (超级管理员/公共账号传 NULL)',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用 1=正常 2=锁定',
    `email` VARCHAR
(
    128
) NULL COMMENT '邮箱',
    `phone` VARCHAR
(
    32
) NULL COMMENT '手机号',
    `expire_at` DATETIME NULL COMMENT '账号过期时间 (临时账号/试用期账号使用)',
    `last_login_at` DATETIME NULL COMMENT '最近登录时间',
    `last_login_ip` VARCHAR
(
    64
) NULL COMMENT '最近登录 IP',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` VARCHAR
(
    64
) NULL COMMENT '创建人 userId',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` VARCHAR
(
    64
) NULL COMMENT '更新人 userId',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_account_no`
(
    `account_no`
),
    UNIQUE KEY `uk_username_tenant`
(
    `username`,
    `tenant_code`
),
    KEY `idx_tenant_code`
(
    `tenant_code`
),
    KEY `idx_status`
(
    `status`
),
    KEY `idx_expire_at`
(
    `expire_at`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='z-ctc 账号主表';

-- 凭证表 (一个账号可绑定多种身份: 用户名+密码 / 手机号验证码 / 邮箱 / 第三方 unionid)
-- user_id 不是 account_id: AuthDO (FEATURE049 起) 映射的是 user_id,
-- 指向 z_ctc_ac_user.id。这份 DDL 一直停在 F037 改名前的形状, 于是干净库上
-- 第一次注册就报 Unknown column 'user_id' in 'field list'。
CREATE TABLE IF NOT EXISTS `z_ctc_ac_auth`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `user_id`
    BIGINT
    NOT
    NULL
    COMMENT
    '用户 ID (关联 z_ctc_ac_user.id)',
    `identity_type`
    TINYINT
    NOT
    NULL
    COMMENT
    '凭证类型: 1=密码 2=手机 3=邮箱 4=微信 unionid 5=钉钉 6=企业微信',
    `identifier`
    VARCHAR
(
    128
) NOT NULL COMMENT '凭证标识 (用户名/手机号/邮箱/unionid)',
    `credential` VARCHAR
(
    256
) NULL COMMENT '凭证值 (密码哈希/手机验证码/unionid 完整值)',
    `salt` VARCHAR
(
    64
) NULL COMMENT '盐 (历史兼容)',
    `credential_strength` TINYINT NULL COMMENT '密码强度: 0=弱 1=中 2=强 (仅密码凭证使用)',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用 1=正常',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_identity`
(
    `identity_type`,
    `identifier`
),
    KEY `idx_user_id`
(
    `user_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='z-ctc 凭证表';

-- 登录日志
CREATE TABLE IF NOT EXISTS `z_ctc_ac_login_log`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `account_id`
    BIGINT
    NULL
    COMMENT
    '账号 ID (登录失败时可能为空)',
    `identifier`
    VARCHAR
(
    128
) NOT NULL COMMENT '登录标识 (用户名/手机号等)',
    `identity_type` TINYINT NOT NULL COMMENT '凭证类型 (同 ac_auth.identity_type)',
    `login_status` TINYINT NOT NULL COMMENT '登录结果: 0=失败 1=成功',
    `failure_reason` VARCHAR
(
    256
) NULL COMMENT '失败原因 (密码错误/账号锁定/账号过期等)',
    `client_ip` VARCHAR
(
    64
) NULL COMMENT '客户端 IP',
    `user_agent` VARCHAR
(
    512
) NULL COMMENT 'UA',
    `tenant_code` VARCHAR
(
    64
) NULL COMMENT '租户',
    `login_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_account_id`
(
    `account_id`
),
    KEY `idx_login_time`
(
    `login_time`
),
    KEY `idx_login_status`
(
    `login_status`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='z-ctc 登录日志';

-- 租户
CREATE TABLE IF NOT EXISTS `z_ctc_ac_tenant`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `tenant_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '租户编码 (业务唯一标识)',
    `tenant_name` VARCHAR
(
    128
) NOT NULL COMMENT '租户名',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用 1=正常',
    `contact_name` VARCHAR
(
    64
) NULL COMMENT '联系人',
    `contact_phone` VARCHAR
(
    32
) NULL COMMENT '联系电话',
    `contact_email` VARCHAR
(
    128
) NULL COMMENT '联系邮箱',
    `expire_at` DATETIME NULL COMMENT '租户过期时间 (SaaS 场景使用)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` VARCHAR
(
    64
) NULL COMMENT '创建人 userId',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_tenant_code`
(
    `tenant_code`
),
    KEY `idx_status`
(
    `status`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='z-ctc 租户表';

-- 组织 (挂在 tenant+domain 下)
CREATE TABLE IF NOT EXISTS `z_ctc_ac_org`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `tenant_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '租户编码',
    `domain_code` VARCHAR
(
    64
) NOT NULL COMMENT '域编码',
    `org_code` VARCHAR
(
    64
) NOT NULL COMMENT '组织编码 (租户域内唯一)',
    `org_name` VARCHAR
(
    128
) NOT NULL COMMENT '组织名称',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0=停用 1=正常',
    `description` VARCHAR
(
    512
) NULL COMMENT '描述',
    `ext_config` TEXT NULL COMMENT '扩展配置 (JSON 字符串)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` VARCHAR
(
    64
) NULL COMMENT '创建人 userId',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` VARCHAR
(
    64
) NULL COMMENT '更新人 userId',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_tenant_domain_org`
(
    `tenant_code`,
    `domain_code`,
    `org_code`
),
    KEY `idx_org_status`
(
    `status`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='z-ctc 组织表';

-- 部门 (挂在组织下)
CREATE TABLE IF NOT EXISTS `z_ctc_ac_dept`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `tenant_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '租户编码',
    `domain_code` VARCHAR
(
    64
) NOT NULL COMMENT '域编码',
    `org_code` VARCHAR
(
    64
) NOT NULL COMMENT '所属组织编码',
    `dept_code` VARCHAR
(
    64
) NOT NULL COMMENT '部门编码 (租户域内唯一)',
    `dept_name` VARCHAR
(
    128
) NOT NULL COMMENT '部门名称',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0=停用 1=正常',
    `description` VARCHAR
(
    512
) NULL COMMENT '描述',
    `ext_config` TEXT NULL COMMENT '扩展配置 (JSON 字符串)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` VARCHAR
(
    64
) NULL COMMENT '创建人 userId',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` VARCHAR
(
    64
) NULL COMMENT '更新人 userId',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_tenant_domain_dept`
(
    `tenant_code`,
    `domain_code`,
    `dept_code`
),
    KEY `idx_dept_org`
(
    `org_code`
),
    KEY `idx_dept_status`
(
    `status`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='z-ctc 部门表';

-- 组别 (挂在部门下)
CREATE TABLE IF NOT EXISTS `z_ctc_ac_group`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `tenant_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '租户编码',
    `domain_code` VARCHAR
(
    64
) NOT NULL COMMENT '域编码',
    `org_code` VARCHAR
(
    64
) NOT NULL COMMENT '所属组织编码',
    `dept_code` VARCHAR
(
    64
) NOT NULL COMMENT '所属部门编码',
    `group_code` VARCHAR
(
    64
) NOT NULL COMMENT '组别编码 (租户域内唯一)',
    `group_name` VARCHAR
(
    128
) NOT NULL COMMENT '组别名称',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0=停用 1=正常',
    `description` VARCHAR
(
    512
) NULL COMMENT '描述',
    `ext_config` TEXT NULL COMMENT '扩展配置 (JSON 字符串)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` VARCHAR
(
    64
) NULL COMMENT '创建人 userId',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by` VARCHAR
(
    64
) NULL COMMENT '更新人 userId',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_tenant_domain_group`
(
    `tenant_code`,
    `domain_code`,
    `group_code`
),
    KEY `idx_group_dept`
(
    `dept_code`
),
    KEY `idx_group_status`
(
    `status`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='z-ctc 组别表';


-- ==== 2. spc 权限表 ====

-- =============================================================================
-- z-ctc-authz 细粒度授权域 SQL (spc 域)
--
-- 5 张表: 角色 / 资源 / 角色-资源关联 / 用户-角色关联 / 数据权限表达式.
-- z-opc 范围内统一前缀 z_ctc_* 风格.
-- =============================================================================

-- 角色
CREATE TABLE IF NOT EXISTS `z_ctc_spc_role`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `role_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '角色编码 (业务唯一, 如 admin / user)',
    `role_name` VARCHAR
(
    128
) NOT NULL COMMENT '角色名',
    `parent_role_id` BIGINT NULL COMMENT '父角色 ID (角色继承)',
    `tenant_code` VARCHAR
(
    64
) NULL COMMENT '租户 (NULL=公共角色)',
    `description` VARCHAR
(
    512
) NULL COMMENT '角色描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用 1=正常',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` VARCHAR
(
    64
) NULL COMMENT '创建人',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_role_code_tenant`
(
    `role_code`,
    `tenant_code`
),
    KEY `idx_parent_role_id`
(
    `parent_role_id`
),
    KEY `idx_tenant_code`
(
    `tenant_code`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='z-ctc 角色表';

-- 资源 (1=菜单/功能权限 2=数据权限表达式 3=API 接口 4=按钮)
CREATE TABLE IF NOT EXISTS `z_ctc_spc_resource`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `resource_code`
    VARCHAR
(
    128
) NOT NULL COMMENT '资源编码 (业务唯一, 如 user:create / order:list)',
    `resource_name` VARCHAR
(
    128
) NOT NULL COMMENT '资源名',
    `resource_type` TINYINT NOT NULL COMMENT '资源类型: 1=菜单/功能 2=数据权限 3=API 接口 4=按钮',
    `app_code` VARCHAR
(
    64
) NULL COMMENT '所属应用编码（NULL=公共/跨应用）— FEATURE015 新增',
    `parent_id` BIGINT NULL COMMENT '父资源 ID (菜单树)',
    `path` VARCHAR
(
    256
) NULL COMMENT '路径 (菜单/接口 URL)',
    `method` VARCHAR
(
    16
) NULL COMMENT 'HTTP 方法 (GET/POST/...) — 接口资源用',
    `icon` VARCHAR
(
    64
) NULL COMMENT '图标 (菜单用)',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `tenant_code` VARCHAR
(
    64
) NULL COMMENT '租户',
    `description` VARCHAR
(
    512
) NULL COMMENT '描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用 1=正常',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by` VARCHAR
(
    64
) NULL COMMENT '创建人',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_resource_code_tenant`
(
    `resource_code`,
    `tenant_code`
),
    KEY `idx_parent_id`
(
    `parent_id`
),
    KEY `idx_resource_type`
(
    `resource_type`
),
    KEY `idx_tenant_code`
(
    `tenant_code`
),
    KEY `idx_app_code`
(
    `app_code`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='z-ctc 资源表';

-- 角色-资源 多对多
CREATE TABLE IF NOT EXISTS `z_ctc_spc_role_resource`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `role_id`
    BIGINT
    NOT
    NULL
    COMMENT
    '角色 ID',
    `resource_id`
    BIGINT
    NOT
    NULL
    COMMENT
    '资源 ID',
    `data_filter`
    VARCHAR
(
    2048
) NULL COMMENT '数据权限过滤表达式 (SpEL, 仅 resource_type=2 时生效, 如 #user.tenantCode == #target.tenantCode)',
    `granted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    `granted_by` VARCHAR
(
    64
) NULL COMMENT '授权人',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_role_resource`
(
    `role_id`,
    `resource_id`
),
    KEY `idx_resource_id`
(
    `resource_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='z-ctc 角色-资源关联';

-- 用户-角色 多对多
CREATE TABLE IF NOT EXISTS `z_ctc_spc_user_role`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `user_id`
    BIGINT
    NOT
    NULL
    COMMENT
    '用户 ID (关联 z_ctc_ac_account.id)',
    `role_id`
    BIGINT
    NOT
    NULL
    COMMENT
    '角色 ID',
    `granted_at`
    DATETIME
    NOT
    NULL
    DEFAULT
    CURRENT_TIMESTAMP
    COMMENT
    '授权时间',
    `granted_by`
    VARCHAR
(
    64
) NULL COMMENT '授权人',
    `tenant_code` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '租户编码',
    `expire_at` DATETIME NULL COMMENT '过期时间 (临时授权场景)',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_user_role`
(
    `user_id`,
    `role_id`
),
    KEY `idx_role_id`
(
    `role_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='z-ctc 用户-角色关联';


-- ==== 3. z-opc-schema.sql 摘录 ====

DROP TABLE IF EXISTS `z_ctc_ac_domain`;
CREATE TABLE `z_ctc_ac_domain`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `domain_code` VARCHAR(64)  NOT NULL COMMENT '域编码 (租户内唯一, 如 prod/staging)',
    `domain_name` VARCHAR(128) NOT NULL COMMENT '域名称',
    `tenant_code` VARCHAR(64)  NOT NULL COMMENT '所属租户编码',
    `status`      TINYINT               DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
    `description` VARCHAR(500)          DEFAULT NULL COMMENT '描述',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by`  VARCHAR(64)           DEFAULT NULL COMMENT '创建人',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_domain` (`tenant_code`, `domain_code`),
    KEY           `idx_tenant_code` (`tenant_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='CTC 域表 (FEATURE016: 域是 Org/Dept/Group 的父级分组)';

DROP TABLE IF EXISTS `z_ctc_app_menu`;
CREATE TABLE `z_ctc_app_menu`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `app_code`    VARCHAR(64)  NOT NULL COMMENT '所属应用编码',
    `menu_code`   VARCHAR(128) NOT NULL COMMENT '菜单编码',
    `menu_name`   VARCHAR(128) NOT NULL COMMENT '菜单名称',
    `parent_code` VARCHAR(128)          DEFAULT NULL COMMENT '父菜单编码',
    `menu_type`   VARCHAR(32)           DEFAULT 'MENU' COMMENT 'MENU/GROUP/IFRAME/LINK',
    `icon`        VARCHAR(64)           DEFAULT NULL COMMENT '图标',
    `path`        VARCHAR(255)          DEFAULT NULL COMMENT '路由路径',
    `sort_order`  INT                   DEFAULT 0 COMMENT '排序号',
    `status`      TINYINT               DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    `source_type` VARCHAR(32)           DEFAULT 'CODE' COMMENT 'CODE/LOW_CODE',
    `page_id`     BIGINT                DEFAULT NULL COMMENT '低代码页面ID (LOW_CODE 类型使用)',
    `component`   VARCHAR(255)          DEFAULT NULL COMMENT '前端组件名',
    `description` VARCHAR(500)          DEFAULT NULL COMMENT '描述',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_menu_code` (`app_code`, `menu_code`),
    KEY           `idx_app_code` (`app_code`),
    KEY           `idx_parent_code` (`parent_code`),
    KEY           `idx_status_sort` (`status`, `sort_order`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='CTC 应用菜单表 (微前端动态菜单)';

DROP TABLE IF EXISTS `z_ctc_verify_code`;
CREATE TABLE `z_ctc_verify_code`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `biz_type`     VARCHAR(32)  NOT NULL COMMENT '业务类型: REGISTER/LOGIN/RESET_PWD',
    `code_type`    VARCHAR(32)  NOT NULL COMMENT '验证码类型: PHONE/EMAIL',
    `receiver`     VARCHAR(128) NOT NULL COMMENT '接收者(手机号/邮箱)',
    `code`         VARCHAR(16)  NOT NULL COMMENT '验证码',
    `expire_time`  DATETIME     NOT NULL COMMENT '过期时间',
    `used`         TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已使用 0-否 1-是',
    `gmt_create`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY            `idx_receiver_expire` (`receiver`, `expire_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='CTC 验证码记录表';

DROP TABLE IF EXISTS `z_ctc_surl_mapping`;
CREATE TABLE `z_ctc_surl_mapping`
(
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `map_key`     VARCHAR(64)   NOT NULL COMMENT '短链 key (URL 路径中的唯一标识)',
    `origin_url`  VARCHAR(2048) NOT NULL COMMENT '原始长链 URL',
    `tenant_code` VARCHAR(64)   NOT NULL DEFAULT 'default' COMMENT '所属租户编码',
    `expire_at`   DATETIME               DEFAULT NULL COMMENT '过期时间 (NULL=永不过期)',
    `hits`        BIGINT                 DEFAULT 0 COMMENT '访问次数',
    `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by`  VARCHAR(64)            DEFAULT NULL COMMENT '创建人',
    `remark`      VARCHAR(500)           DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_map_key` (`map_key`),
    KEY           `idx_tenant_code` (`tenant_code`),
    KEY           `idx_expire_at` (`expire_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='CTC 短链映射表';

-- ==== 4. spc deny/grant + app_menu 加列 (依赖 3) ====

-- =============================================================================
-- z-ctc-authz RBAC + Perm Override 扩展
--
-- 在原 RBAC 四表基础上新增两张表:
--   z_ctc_spc_user_grant — 用户直接授权 (白名单, 加集)
--   z_ctc_spc_user_deny  — 用户直接拒绝 (黑名单, 减集)
--
-- 最终权限 = 角色权限(含继承) ∪ user_grant - user_deny
--
-- 幂等性: CREATE TABLE IF NOT EXISTS, 多次执行安全
-- =============================================================================

SET NAMES utf8mb4;

-- =============================================================================
-- 0. z_ctc_app_menu 加 resource_id 列 (菜单-资源绑定)
--    NULL=无权限控制, 非NULL=需要对应 resource_id 的权限才可见
-- =============================================================================
ALTER TABLE `z_ctc_app_menu`
    ADD COLUMN `resource_id` BIGINT NULL COMMENT '关联权限资源 ID (NULL=无权限控制)' AFTER `description`;

-- =============================================================================
-- 1. 用户直接授权 (白名单, 加集)
--    场景: 用户除了角色带来的权限外, 额外被授予某些权限
-- =============================================================================
CREATE TABLE IF NOT EXISTS `z_ctc_spc_user_grant`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL COMMENT '用户 ID (关联 z_ctc_ac_user.id)',
    `resource_id` BIGINT       NOT NULL COMMENT '资源 ID (关联 z_ctc_spc_resource.id)',
    `tenant_code` VARCHAR(64)  NULL     COMMENT '租户编码 (隔离)',
    `expire_at`   DATETIME     NULL     COMMENT '过期时间 (NULL=永久)',
    `granted_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    `granted_by`  VARCHAR(64)  NULL     COMMENT '授权人',
    `description` VARCHAR(512) NULL     COMMENT '授权理由',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_resource_grant` (`user_id`, `resource_id`),
    KEY `idx_resource_id` (`resource_id`),
    KEY `idx_tenant_code` (`tenant_code`),
    KEY `idx_expire_at` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='z-ctc 用户直接授权 (RBAC+Override 白名单)';

-- =============================================================================
-- 2. 用户直接拒绝 (黑名单, 减集)
--    场景: 用户从角色继承了某权限, 但管理员明确要屏蔽它
-- =============================================================================
CREATE TABLE IF NOT EXISTS `z_ctc_spc_user_deny`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL COMMENT '用户 ID (关联 z_ctc_ac_user.id)',
    `resource_id` BIGINT       NOT NULL COMMENT '资源 ID (关联 z_ctc_spc_resource.id)',
    `tenant_code` VARCHAR(64)  NULL     COMMENT '租户编码 (隔离)',
    `expire_at`   DATETIME     NULL     COMMENT '过期时间 (NULL=永久)',
    `denied_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拒绝时间',
    `denied_by`   VARCHAR(64)  NULL     COMMENT '操作人',
    `description` VARCHAR(512) NULL     COMMENT '拒绝理由',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_resource_deny` (`user_id`, `resource_id`),
    KEY `idx_resource_id` (`resource_id`),
    KEY `idx_tenant_code` (`tenant_code`),
    KEY `idx_expire_at` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='z-ctc 用户直接拒绝 (RBAC+Override 黑名单)';


-- ==== 5. user/membership/invitation (依赖 1、2) ====

-- =============================================================================
-- FEATURE049 · z-ctc 多租户身份模型重构 (User + Membership) - v2
-- 创建时间: 2026-07-14
-- 优先级: P0
-- 关联: z-ctc (ac / authz / sso), F037 (租户/域字段统一迁移)
--
-- 说明:
--   F037 已经在 z_ctc_ac_auth / z_ctc_spc_user_role 加了 tenant_code, domain_code
--   并将 account_id 改名为 user_id. 本脚本只做 FEATURE049 独有的部分.
--
-- 本脚本只做:
--   新建: z_ctc_ac_user, z_ctc_ac_membership, z_ctc_ac_invitation
--   从 z_ctc_ac_account 拆出 z_ctc_ac_user
--   从 z_ctc_ac_account 生成 z_ctc_ac_membership
--   z_ctc_spc_user_role 唯一键改为三元组 (user_id, role_id, tenant_code)
--   标记 z_ctc_ac_account 为 deprecated
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 步骤 1: 新建 z_ctc_ac_user (全局身份)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_ctc_ac_user`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `user_no`
    VARCHAR
(
    64
) NOT NULL COMMENT '用户编号 (业务可读唯一标识)',
    `email` VARCHAR
(
    128
) NULL COMMENT '邮箱 (全局唯一, 登录用)',
    `phone` VARCHAR
(
    32
) NULL COMMENT '手机号 (全局唯一, 登录用)',
    `username` VARCHAR
(
    64
) NULL COMMENT '用户名 (可选, 非唯一, 仅显示用)',
    `nickname` VARCHAR
(
    64
) NULL COMMENT '昵称',
    `avatar` VARCHAR
(
    512
) NULL COMMENT '头像 URL',
    `password_hash` VARCHAR
(
    256
) NULL COMMENT 'BCrypt 密码哈希',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '全局状态: 0=禁用 1=正常 2=锁定',
    `last_login_at` DATETIME NULL COMMENT '最近登录时间',
    `last_login_ip` VARCHAR
(
    64
) NULL COMMENT '最近登录 IP',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_user_no`
(
    `user_no`
),
    UNIQUE KEY `uk_email`
(
    `email`
),
    UNIQUE KEY `uk_phone`
(
    `phone`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='z-ctc 全局用户表 (身份层, 不绑租户)';

-- -----------------------------------------------------------------------------
-- 步骤 2: 从 z_ctc_ac_account 拆出 user (身份信息, 一一对应)
--   用 INSERT IGNORE, 跳过 email/phone 冲突的 (后续手动处理)
-- -----------------------------------------------------------------------------
INSERT
IGNORE INTO `z_ctc_ac_user` (id, user_no, email, phone, username, nickname,
                                     password_hash, status, last_login_at, last_login_ip,
                                     created_at, updated_at)
SELECT id,
       account_no,
       email,
       phone,
       username,
       nickname,
       password_hash,
       status,
       last_login_at,
       last_login_ip,
       created_at,
       updated_at
FROM `z_ctc_ac_account`;

-- 步骤 2b: 补漏 (如果有 INSERT IGNORE 跳过的, 用空 phone 强制插入)
INSERT INTO `z_ctc_ac_user` (id, user_no, email, phone, username, nickname,
                             password_hash, status, last_login_at, last_login_ip,
                             created_at, updated_at)
SELECT a.id,
       a.account_no,
       NULL,
       NULL,
       a.username,
       a.nickname,
       a.password_hash,
       a.status,
       a.last_login_at,
       a.last_login_ip,
       a.created_at,
       a.updated_at
FROM `z_ctc_ac_account` a
         LEFT JOIN `z_ctc_ac_user` u ON u.id = a.id
WHERE u.id IS NULL;

-- -----------------------------------------------------------------------------
-- 步骤 3: 新建 z_ctc_ac_membership (User x Tenant 多对多)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_ctc_ac_membership`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `user_id`
    BIGINT
    NOT
    NULL
    COMMENT
    '用户 ID (关联 z_ctc_ac_user.id)',
    `tenant_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '租户编码',
    `role_code` VARCHAR
(
    64
) NULL COMMENT '租户内角色编码 (冗余, 加速鉴权)',
    `dept_code` VARCHAR
(
    64
) NULL COMMENT '所属部门 (组织树)',
    `member_status` TINYINT NOT NULL DEFAULT 0 COMMENT '成员状态: 0=待接受 1=正常 2=已禁用 3=已退出',
    `joined_at` DATETIME NULL COMMENT '加入时间 (status→1 时写入)',
    `left_at` DATETIME NULL COMMENT '退出时间 (status→3 时写入)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_user_tenant`
(
    `user_id`,
    `tenant_code`
),
    KEY `idx_tenant_code`
(
    `tenant_code`
),
    KEY `idx_user_id`
(
    `user_id`
),
    KEY `idx_member_status`
(
    `member_status`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='z-ctc 租户成员关系表 (User×Tenant 多对多)';

-- -----------------------------------------------------------------------------
-- 步骤 4: 从 z_ctc_ac_account 生成 membership (tenant_code 非空的)
-- -----------------------------------------------------------------------------
INSERT
IGNORE INTO `z_ctc_ac_membership` (user_id, tenant_code, role_code, member_status,
                                           joined_at, created_at, updated_at)
SELECT id,
       tenant_code,
       CASE account_type WHEN 1 THEN 'owner' WHEN 2 THEN 'admin' ELSE 'member' END,
       1,
       created_at,
       created_at,
       updated_at
FROM `z_ctc_ac_account`
WHERE tenant_code IS NOT NULL;

-- -----------------------------------------------------------------------------
-- 步骤 5: 新建 z_ctc_ac_invitation (邀请记录)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_ctc_ac_invitation`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `tenant_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '邀请来源租户',
    `inviter_user_id` BIGINT NOT NULL COMMENT '邀请人 user_id',
    `invitee_email` VARCHAR
(
    128
) NULL COMMENT '被邀请人邮箱 (邀请链接用)',
    `invitee_phone` VARCHAR
(
    32
) NULL COMMENT '被邀请人手机号',
    `invite_code` VARCHAR
(
    32
) NULL COMMENT '邀请码 (手动输入用, 6-8位)',
    `token` VARCHAR
(
    128
) NULL COMMENT '邀请链接 token (UUID)',
    `role_code` VARCHAR
(
    64
) NULL COMMENT '指定角色',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0=待接受 1=已接受 2=已拒绝 3=已过期',
    `max_uses` INT NOT NULL DEFAULT 1 COMMENT '最大使用次数 (邀请码可复用时>1)',
    `used_count` INT NOT NULL DEFAULT 0 COMMENT '已使用次数',
    `expire_at` DATETIME NOT NULL COMMENT '过期时间',
    `accepted_at` DATETIME NULL COMMENT '接受时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_token`
(
    `token`
),
    UNIQUE KEY `uk_invite_code`
(
    `invite_code`
),
    KEY `idx_tenant_code`
(
    `tenant_code`
),
    KEY `idx_invitee_email`
(
    `invitee_email`
),
    KEY `idx_status`
(
    `status`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='z-ctc 租户邀请记录';

-- -----------------------------------------------------------------------------
-- 步骤 6: 改造 z_ctc_spc_user_role 唯一键为三元组
--   F037 已经加了 tenant_code 列, 但唯一键还是 uk_user_role(user_id, role_id)
--   需要改为 uk_user_role_tenant(user_id, role_id, tenant_code)
-- -----------------------------------------------------------------------------
-- 删旧唯一键 (幂等)
SET
@sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'z_ctc_spc_user_role'
     AND index_name = 'uk_user_role') > 0,
    'ALTER TABLE z_ctc_spc_user_role DROP INDEX uk_user_role',
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 加新唯一键 (幂等)
SET
@sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'z_ctc_spc_user_role'
     AND index_name = 'uk_user_role_tenant') = 0,
    'ALTER TABLE z_ctc_spc_user_role ADD UNIQUE KEY uk_user_role_tenant (user_id, role_id, tenant_code)',
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 步骤 7: 标记 z_ctc_ac_account 为 deprecated
-- -----------------------------------------------------------------------------
SET
@sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'z_ctc_ac_account'
     AND column_name = 'migrated') = 0,
    'ALTER TABLE z_ctc_ac_account ADD COLUMN migrated TINYINT NOT NULL DEFAULT 1 COMMENT ''已迁移到 user+membership (0=未迁移 1=已迁移)''',
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 步骤 8: 验证迁移结果
-- -----------------------------------------------------------------------------
SELECT (SELECT COUNT(*) FROM z_ctc_ac_user)                                     AS user_count,
       (SELECT COUNT(*) FROM z_ctc_ac_account)                                  AS account_count,
       (SELECT COUNT(*) FROM z_ctc_ac_membership)                               AS membership_count,
       (SELECT COUNT(*) FROM z_ctc_ac_account WHERE tenant_code IS NOT NULL)    AS accounts_with_tenant,
       (SELECT COUNT(*) FROM z_ctc_ac_auth)                                     AS auth_count,
       (SELECT COUNT(*) FROM z_ctc_spc_user_role)                               AS user_role_count,
       (SELECT COUNT(*) FROM z_ctc_spc_user_role WHERE tenant_code IS NOT NULL) AS user_role_with_tenant_count;

-- 预期结果:
--   user_count == account_count (身份 1:1 迁移)
--   membership_count == accounts_with_tenant
--   user_role_count 不变
--   user_role_with_tenant_count 应该接近 user_role_count
