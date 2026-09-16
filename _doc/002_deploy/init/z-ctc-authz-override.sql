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

USE `oc`;
SET NAMES utf8mb4;

-- =============================================================================
-- 0. z_ctc_app_menu 加 resource_id 列 (菜单-资源绑定)
--    NULL=无权限控制, 非NULL=需要对应 resource_id 的权限才可见
-- =============================================================================
ALTER TABLE `z_ctc_app_menu`
    ADD COLUMN IF NOT EXISTS `resource_id` BIGINT NULL COMMENT '关联权限资源 ID (NULL=无权限控制)' AFTER `description`;

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
