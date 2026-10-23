-- =====================================================================
-- 结构对齐回写 (缺表) — 生成日 2026-10-24
-- 来源: 生产 oc 只读 mysqldump --no-data
-- 口径: CREATE TABLE IF NOT EXISTS, 结构与生产一致;
--       外键已剥离 (被引用的表未必在本仓, 直接搬会建不出表);
--       归属按代码引用取证 (@TableName 实体声明 > 源码出现)。
-- 位置: z-ctc/_doc/002_deploy/init
-- =====================================================================

CREATE TABLE IF NOT EXISTS `z_ctc_audit_log`
(

  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `operation_type` varchar(64) NOT NULL COMMENT '操作类型：LOGIN/LOGOUT/CREATE/UPDATE/DELETE',
  `operation_desc` varchar(256) DEFAULT NULL COMMENT '操作描述',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `user_name` varchar(64) DEFAULT NULL COMMENT '用户名',
  `tenant_code` varchar(64) DEFAULT NULL COMMENT '租户编码',
  `ip_address` varchar(64) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(512) DEFAULT NULL COMMENT '用户代理',
  `request_url` varchar(512) DEFAULT NULL COMMENT '请求URL',
  `request_method` varchar(32) DEFAULT NULL COMMENT '请求方法',
  `request_params` text COMMENT '请求参数',
  `response_data` text COMMENT '响应数据',
  `execution_time` int DEFAULT '0' COMMENT '执行时间（毫秒）',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-失败，1-成功',
  `error_msg` text COMMENT '错误信息',
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `domain_code` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_tenant_code` (`tenant_code`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_gmt_create` (`gmt_create`),
  KEY `idx_tenant_domain` (`tenant_code`,`domain_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `z_ctc_dept`
(

  `id` bigint NOT NULL AUTO_INCREMENT,
  `dept_code` varchar(64) NOT NULL DEFAULT '' COMMENT '部门编码',
  `dept_name` varchar(100) NOT NULL COMMENT '部门名称',
  `tenant_code` varchar(64) DEFAULT NULL COMMENT '租户编码',
  `domain_code` varchar(64) DEFAULT NULL COMMENT '域编码',
  `org_code` varchar(64) DEFAULT NULL COMMENT '组织编码',
  `parent_code` varchar(64) DEFAULT NULL COMMENT '上级部门编码',
  `status` int DEFAULT '1' COMMENT '状态 1-启用 0-禁用',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `ext_config` text,
  `domain_id` bigint DEFAULT NULL COMMENT '域ID',
  `org_id` bigint DEFAULT NULL COMMENT '组织ID',
  `parent_id` bigint DEFAULT '0' COMMENT '上级部门ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_dept_code` (`dept_code`),
  KEY `idx_tenant_code` (`tenant_code`),
  KEY `idx_domain_code` (`domain_code`),
  KEY `idx_org_code` (`org_code`),
  KEY `idx_parent_code` (`parent_code`),
  KEY `idx_tenant_domain` (`tenant_code`,`domain_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `z_ctc_domain`
(

  `id` bigint NOT NULL AUTO_INCREMENT,
  `domain_code` varchar(64) NOT NULL COMMENT '域编码（PK）',
  `domain_name` varchar(100) NOT NULL COMMENT '域名称',
  `tenant_code` varchar(64) DEFAULT NULL COMMENT '租户编码',
  `status` int DEFAULT '1' COMMENT '状态 1-启用 0-禁用',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `ext_config` text COMMENT '扩展K-V配置(JSON)',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_domain_code` (`domain_code`),
  KEY `idx_tenant_code` (`tenant_code`),
  KEY `idx_tenant_domain` (`tenant_code`,`domain_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `z_ctc_group`
(

  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_code` varchar(64) NOT NULL DEFAULT '' COMMENT '组编码',
  `group_name` varchar(100) NOT NULL COMMENT '组名称',
  `tenant_code` varchar(64) DEFAULT NULL COMMENT '租户编码',
  `domain_code` varchar(64) DEFAULT NULL COMMENT '域编码',
  `org_code` varchar(64) DEFAULT NULL COMMENT '组织编码',
  `dept_code` varchar(64) DEFAULT NULL COMMENT '部门编码',
  `status` int DEFAULT '1' COMMENT '状态 1-启用 0-禁用',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `ext_config` text,
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `domain_id` bigint DEFAULT NULL COMMENT '域ID',
  `org_id` bigint DEFAULT NULL COMMENT '组织ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_group_code` (`group_code`),
  KEY `idx_tenant_code` (`tenant_code`),
  KEY `idx_domain_code` (`domain_code`),
  KEY `idx_org_code` (`org_code`),
  KEY `idx_dept_code` (`dept_code`),
  KEY `idx_tenant_domain` (`tenant_code`,`domain_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `z_ctc_login_log`
(

  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `user_name` varchar(64) DEFAULT NULL COMMENT '用户名',
  `tenant_code` varchar(64) DEFAULT NULL COMMENT '租户编码',
  `login_type` varchar(32) DEFAULT 'PASSWORD' COMMENT '登录类型：PASSWORD/SMS/QR_CODE',
  `ip_address` varchar(64) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(512) DEFAULT NULL COMMENT '用户代理',
  `browser` varchar(128) DEFAULT NULL COMMENT '浏览器',
  `os` varchar(128) DEFAULT NULL COMMENT '操作系统',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-失败，1-成功',
  `error_msg` varchar(512) DEFAULT NULL COMMENT '错误信息',
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `domain_code` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_tenant_code` (`tenant_code`),
  KEY `idx_gmt_create` (`gmt_create`),
  KEY `idx_tenant_domain` (`tenant_code`,`domain_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `z_ctc_org`
(

  `id` bigint NOT NULL AUTO_INCREMENT,
  `org_code` varchar(64) NOT NULL DEFAULT '' COMMENT '组织编码（PK）',
  `org_name` varchar(100) NOT NULL COMMENT '组织名称',
  `tenant_code` varchar(64) DEFAULT NULL COMMENT '租户编码',
  `domain_code` varchar(64) DEFAULT NULL COMMENT '域编码',
  `parent_code` varchar(64) DEFAULT NULL COMMENT '上级组织编码',
  `status` int DEFAULT '1' COMMENT '状态 1-启用 0-禁用',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `ext_config` text,
  `domain_id` bigint DEFAULT NULL COMMENT '域ID',
  `parent_id` bigint DEFAULT '0' COMMENT '上级组织ID',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_org_code` (`org_code`),
  KEY `idx_tenant_code` (`tenant_code`),
  KEY `idx_domain_code` (`domain_code`),
  KEY `idx_parent_code` (`parent_code`),
  KEY `idx_tenant_domain` (`tenant_code`,`domain_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `z_ctc_permission`
(

  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `perm_code` varchar(128) NOT NULL COMMENT '权限编码',
  `perm_name` varchar(64) NOT NULL COMMENT '权限名称',
  `perm_type` varchar(32) NOT NULL COMMENT '权限类型：MENU-菜单，BUTTON-按钮，API-接口',
  `parent_id` bigint DEFAULT '0' COMMENT '父权限ID',
  `path` varchar(256) DEFAULT NULL COMMENT '权限路径',
  `icon` varchar(64) DEFAULT NULL COMMENT '图标',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `tenant_code` varchar(64) DEFAULT NULL COMMENT '租户编码',
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `domain_code` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perm_code` (`perm_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_tenant_code` (`tenant_code`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_domain` (`tenant_code`,`domain_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `z_ctc_role`
(

  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code` varchar(64) NOT NULL COMMENT '角色编码',
  `role_name` varchar(64) NOT NULL COMMENT '角色名称',
  `description` varchar(256) DEFAULT NULL COMMENT '角色描述',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `tenant_code` varchar(64) DEFAULT NULL COMMENT '租户编码',
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `domain_code` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_tenant_code` (`tenant_code`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_domain` (`tenant_code`,`domain_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `z_ctc_role_permission`
(

  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_code` varchar(64) DEFAULT NULL,
  `domain_code` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_permission_id` (`permission_id`),
  KEY `idx_tenant_domain` (`tenant_code`,`domain_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `z_ctc_user`
(

  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `user_name` varchar(64) NOT NULL COMMENT '用户名',
  `password` varchar(128) NOT NULL COMMENT '密码',
  `real_name` varchar(64) DEFAULT NULL COMMENT '真实姓名',
  `nick_name` varchar(64) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(32) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(256) DEFAULT NULL,
  `gender` tinyint DEFAULT NULL,
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `tenant_code` varchar(64) DEFAULT NULL COMMENT '租户编码',
  `dept_code` varchar(64) DEFAULT NULL COMMENT '部门编码',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除：0-否，1-是',
  `domain_code` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_name` (`user_name`),
  KEY `idx_tenant_code` (`tenant_code`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_domain` (`tenant_code`,`domain_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `z_ctc_user_role`
(

  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_code` varchar(64) DEFAULT NULL,
  `domain_code` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_tenant_domain` (`tenant_code`,`domain_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
