-- z-ctc 权限类审批域 SQL（H2 测试口径，字段与 _doc/002_deploy/init/z-ctc-approval.sql 一致）

CREATE TABLE IF NOT EXISTS z_ctc_approval_template
(
    id           VARCHAR(64)  NOT NULL PRIMARY KEY,
    tenant_code  VARCHAR(64)  NOT NULL,
    code         VARCHAR(64)  NOT NULL,
    name         VARCHAR(128) NOT NULL,
    version      VARCHAR(16),
    biz_type     VARCHAR(64)  NOT NULL,
    nodes_json   TEXT,
    published_at TIMESTAMP(3),
    published_by VARCHAR(64),
    is_deleted   INT          NOT NULL DEFAULT 0,
    create_by    VARCHAR(64),
    update_by    VARCHAR(64),
    create_time  TIMESTAMP(3),
    update_time  TIMESTAMP(3)
);

CREATE TABLE IF NOT EXISTS z_ctc_approval_ticket
(
    id                    VARCHAR(64)   NOT NULL PRIMARY KEY,
    tenant_code           VARCHAR(64)   NOT NULL,
    code                  VARCHAR(64)   NOT NULL,
    title                 VARCHAR(256),
    template_code         VARCHAR(64),
    template_name         VARCHAR(128),
    biz_type              VARCHAR(64)   NOT NULL,
    biz_ref               VARCHAR(128),
    biz_name              VARCHAR(256),
    biz_role              VARCHAR(64),
    applicant_id          VARCHAR(64),
    applicant_name        VARCHAR(128),
    resource_codes_json   TEXT,
    status                VARCHAR(32)   NOT NULL,
    current_node          INT           DEFAULT 0,
    current_approver_name VARCHAR(512),
    reason                VARCHAR(1024),
    nodes_json            TEXT,
    finished_at           TIMESTAMP(3),
    is_deleted            INT           NOT NULL DEFAULT 0,
    create_by             VARCHAR(64),
    update_by             VARCHAR(64),
    create_time           TIMESTAMP(3),
    update_time           TIMESTAMP(3)
);
