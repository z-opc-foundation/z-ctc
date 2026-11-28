-- z-ctc AK 台账域 SQL（MySQL 8.0）
-- 口径：secret 只存 SHA-256 哈希，明文仅签发那一次响应返回；调用日志只落脱敏后的载荷。

CREATE TABLE IF NOT EXISTS `z_ctc_ak`
(
    `id`          VARCHAR(64) NOT NULL COMMENT '主键',
    `tenant_code` VARCHAR(64) NOT NULL COMMENT '租户编码',
    `ak_code`     VARCHAR(64) NOT NULL COMMENT 'AK 编码（对外引用用）',
    `ak_name`     VARCHAR(128) NOT NULL COMMENT 'AK 名称',
    `owner_type`  VARCHAR(16)  NOT NULL COMMENT '归属类型：PERSONAL / APPLICATION',
    `owner_id`    VARCHAR(64)  NULL COMMENT '归属人标识',
    `owner_name`  VARCHAR(128) NULL COMMENT '归属人姓名',
    `secret_hash` VARCHAR(128) NOT NULL COMMENT '密钥哈希（SHA-256），不存明文',
    `status`      INT          NOT NULL DEFAULT 1 COMMENT '状态 1-启用',
    `is_deleted`  INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_by`   VARCHAR(64)  NULL,
    `update_by`   VARCHAR(64)  NULL,
    `create_time` TIMESTAMP(3) NULL,
    `update_time` TIMESTAMP(3) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_z_ctc_ak_owner` (`tenant_code`, `owner_type`, `owner_id`, `is_deleted`),
    KEY `idx_z_ctc_ak_code` (`tenant_code`, `ak_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `z_ctc_ak_scope`
(
    `id`            VARCHAR(64)  NOT NULL COMMENT '主键',
    `tenant_code`   VARCHAR(64)  NOT NULL,
    `ak_code`       VARCHAR(64)  NOT NULL,
    `domain`        VARCHAR(64)  NOT NULL COMMENT '授权域',
    `action`        VARCHAR(64)  NOT NULL COMMENT '动作',
    `resource`      VARCHAR(512) NOT NULL COMMENT '资源（* 表示全部）',
    `resource_name` VARCHAR(255) NULL,
    `create_time`   TIMESTAMP(3) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_z_ctc_ak_scope` (`tenant_code`, `ak_code`, `domain`, `action`, `resource`),
    KEY `idx_z_ctc_ak_scope_lookup` (`tenant_code`, `ak_code`, `domain`, `action`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `z_ctc_ak_call_log`
(
    `id`                  VARCHAR(64)   NOT NULL COMMENT '主键',
    `tenant_code`         VARCHAR(64)   NOT NULL,
    `ak_id`               VARCHAR(64)   NOT NULL,
    `ak_code`             VARCHAR(64)   NOT NULL,
    `ak_masked`           VARCHAR(128)  NOT NULL COMMENT '掩码后的 AK',
    `ak_name`             VARCHAR(128)  NULL,
    `owner_id`            VARCHAR(64)   NULL,
    `owner_name`          VARCHAR(128)  NULL,
    `trace_id`            VARCHAR(128)  NULL,
    `started_at`          TIMESTAMP(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '开始时刻（RDS 关 explicit_defaults_for_timestamp 时，无默认的第二个 TIMESTAMP 会落到零值默认被拒）',
    `completed_at`        TIMESTAMP(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `duration_ms`         BIGINT        NOT NULL,
    `http_method`         VARCHAR(16)   NOT NULL,
    `request_path`        VARCHAR(1024) NOT NULL,
    `query`               TEXT          NULL,
    `request_headers_json` TEXT         NULL,
    `request_body`        TEXT          NULL,
    `response_body`       TEXT          NULL,
    `http_status`         INT           NOT NULL,
    `outcome`             VARCHAR(32)   NOT NULL,
    `error_summary`       VARCHAR(1000) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_z_ctc_ak_log_page` (`tenant_code`, `ak_code`, `completed_at`, `id`),
    KEY `idx_z_ctc_ak_log_detail` (`tenant_code`, `id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
