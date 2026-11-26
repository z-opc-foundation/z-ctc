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
