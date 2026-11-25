-- =============================================================================
-- z-ctc 建站初始化域 (site-init) SQL (MySQL 8.0 + Druid 1.2.18)
--
-- 2 张表:
--   z_ctc_site_init_registration  域注册表 (各域把自己的初始化能力登记到 4A 中心)
--   z_ctc_site_init_task          初始化任务记录 (站点 × 域 × 能力 的执行明细)
--
-- 对应实体:
--   SiteInitRegistrationDO (z-ctc-core/domain/entity)
--   SiteInitTaskDO         (z-ctc-core/domain/entity)
-- =============================================================================

-- 建站初始化域注册表
CREATE TABLE IF NOT EXISTS `z_ctc_site_init_registration`
(
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `domain_code`       VARCHAR(64)  NOT NULL COMMENT '域编码 (全平台唯一, 如 z-cognition)',
    `domain_name`       VARCHAR(128) NOT NULL DEFAULT '' COMMENT '域名称',
    `order_no`          INT          NOT NULL DEFAULT 100 COMMENT '跨域执行顺序, 数字小者先跑',
    `endpoint`          VARCHAR(512) NOT NULL COMMENT '域初始化服务根地址 (ctc 按标准协议调 {endpoint}/api/site-init/*)',
    `protocol_version`  VARCHAR(16)  NOT NULL DEFAULT '1.0' COMMENT '协议版本',
    `capabilities`      VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '能力编码清单 (逗号分隔)',
    `status`            TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=停用 1=启用',
    `registered_at`     DATETIME     NULL COMMENT '最近注册时间',
    `created_at`        DATETIME     NULL COMMENT '创建时间',
    `updated_at`        DATETIME     NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_domain_code` (`domain_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '建站初始化域注册';

-- 建站初始化任务记录
CREATE TABLE IF NOT EXISTS `z_ctc_site_init_task`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_code`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '站点(租户)编码',
    `domain_code`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '域编码',
    `capability`      VARCHAR(128) NOT NULL DEFAULT '' COMMENT '能力编码 (空 = 该域全量能力)',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0=待执行 1=执行中 2=成功 3=失败 4=跳过',
    `idempotency_key` VARCHAR(191) NOT NULL DEFAULT '' COMMENT '幂等键',
    `request_json`    TEXT         NULL COMMENT '请求报文 (JSON)',
    `response_json`   TEXT         NULL COMMENT '响应报文 (JSON)',
    `message`         VARCHAR(1024) NULL COMMENT '结果说明 / 失败原因',
    `cost_ms`         BIGINT       NULL COMMENT '耗时 (毫秒)',
    `trace_id`        VARCHAR(64)  NULL COMMENT '链路追踪 id',
    `created_at`      DATETIME     NULL COMMENT '创建时间',
    `updated_at`      DATETIME     NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_code` (`tenant_code`),
    KEY `idx_idempotency_key` (`idempotency_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '建站初始化任务';
