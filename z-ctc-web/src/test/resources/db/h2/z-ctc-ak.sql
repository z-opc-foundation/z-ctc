-- z-ctc AK 台账域 SQL（H2 测试口径，字段与 _doc/002_deploy/init/z-ctc-ak.sql 一致）
-- 口径：secret 只存 SHA-256 哈希，明文仅签发那一次响应返回；调用日志只落脱敏后的载荷。

CREATE TABLE IF NOT EXISTS z_ctc_ak
(
    id          VARCHAR(64)  NOT NULL PRIMARY KEY,
    tenant_code VARCHAR(64)  NOT NULL,
    ak_code     VARCHAR(64)  NOT NULL,
    ak_name     VARCHAR(128) NOT NULL,
    owner_type  VARCHAR(16)  NOT NULL,
    owner_id    VARCHAR(64),
    owner_name  VARCHAR(128),
    secret_hash VARCHAR(128) NOT NULL,
    status      INT          NOT NULL DEFAULT 1,
    is_deleted  INT          NOT NULL DEFAULT 0,
    create_by   VARCHAR(64),
    update_by   VARCHAR(64),
    create_time TIMESTAMP(3),
    update_time TIMESTAMP(3)
);

CREATE TABLE IF NOT EXISTS z_ctc_ak_scope
(
    id            VARCHAR(64)  NOT NULL PRIMARY KEY,
    tenant_code   VARCHAR(64)  NOT NULL,
    ak_code       VARCHAR(64)  NOT NULL,
    domain        VARCHAR(64)  NOT NULL,
    action        VARCHAR(64)  NOT NULL,
    resource      VARCHAR(512) NOT NULL,
    resource_name VARCHAR(255),
    create_time   TIMESTAMP(3)
);

CREATE TABLE IF NOT EXISTS z_ctc_ak_call_log
(
    id                  VARCHAR(64)   NOT NULL PRIMARY KEY,
    tenant_code         VARCHAR(64)   NOT NULL,
    ak_id               VARCHAR(64)   NOT NULL,
    ak_code             VARCHAR(64)   NOT NULL,
    ak_masked           VARCHAR(128)  NOT NULL,
    ak_name             VARCHAR(128),
    owner_id            VARCHAR(64),
    owner_name          VARCHAR(128),
    trace_id            VARCHAR(128),
    started_at          TIMESTAMP(3)  NOT NULL,
    completed_at        TIMESTAMP(3)  NOT NULL,
    duration_ms         BIGINT        NOT NULL,
    http_method         VARCHAR(16)   NOT NULL,
    request_path        VARCHAR(1024) NOT NULL,
    query               CLOB,
    request_headers_json CLOB,
    request_body        CLOB,
    response_body       CLOB,
    http_status         INT           NOT NULL,
    outcome             VARCHAR(32)   NOT NULL,
    error_summary       VARCHAR(1000)
);
