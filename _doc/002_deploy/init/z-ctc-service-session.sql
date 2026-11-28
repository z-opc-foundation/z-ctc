-- z-ctc 机器 / 服务账号会话 SQL（MySQL 8.0）
-- 口径：令牌只存 SHA-256 哈希，明文仅签发那一次响应返回。

CREATE TABLE IF NOT EXISTS `z_ctc_service_session`
(
    `id`           VARCHAR(64)  NOT NULL COMMENT '主键',
    `tenant_code`  VARCHAR(64)  NOT NULL COMMENT '租户编码',
    `subject_type` VARCHAR(16)  NOT NULL COMMENT '主体类型：DEVICE / SERVICE',
    `subject_code` VARCHAR(64)  NOT NULL COMMENT '主体标识（设备号 / 服务名）',
    `token_hash`   VARCHAR(128) NOT NULL COMMENT '令牌哈希（SHA-256），不存明文',
    `issued_at`    BIGINT       NOT NULL COMMENT '签发时间（毫秒）',
    `expires_at`   BIGINT       NOT NULL COMMENT '过期时间（毫秒）',
    `last_seen_at` BIGINT       NOT NULL COMMENT '最近活跃时间（毫秒）',
    `revoked`      INT          NOT NULL DEFAULT 0 COMMENT '是否吊销',
    `create_time`  BIGINT       NOT NULL COMMENT '创建时间（毫秒）',
    PRIMARY KEY (`id`),
    KEY `idx_z_ctc_service_session_token` (`token_hash`),
    KEY `idx_z_ctc_service_session_tenant` (`tenant_code`, `revoked`, `expires_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
