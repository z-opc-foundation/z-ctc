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
CREATE TABLE IF NOT EXISTS `z_ctc_ac_auth`
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
    NOT
    NULL
    COMMENT
    '账号 ID (关联 z_ctc_ac_account.id)',
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
    KEY `idx_account_id`
(
    `account_id`
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
