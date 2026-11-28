-- z-ctc 机器 / 服务账号会话 SQL（H2 测试口径，字段与 MySQL 版一致）
-- 口径：令牌只存 SHA-256 哈希，明文仅签发那一次响应返回。

CREATE TABLE IF NOT EXISTS z_ctc_service_session
(
    id           VARCHAR(64)  NOT NULL PRIMARY KEY,
    tenant_code  VARCHAR(64)  NOT NULL,
    subject_type VARCHAR(16)  NOT NULL,
    subject_code VARCHAR(64)  NOT NULL,
    token_hash   VARCHAR(128) NOT NULL,
    issued_at    BIGINT       NOT NULL,
    expires_at   BIGINT       NOT NULL,
    last_seen_at BIGINT       NOT NULL,
    revoked      INT          NOT NULL DEFAULT 0,
    create_time  BIGINT       NOT NULL
);
