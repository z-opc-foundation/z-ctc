-- z-ctc 权限类审批域 SQL（MySQL 8.0）
-- 口径（2026-11-28 审批分家拍板）：权限相关的审批真源在 z-ctc；复杂业务审批归 z-wf。
-- 引擎对 bizType 透明：模板/票由宿主按自己的审批人语义展开，授权副作用由宿主落地。

CREATE TABLE IF NOT EXISTS `z_ctc_approval_template`
(
    `id`           VARCHAR(64)  NOT NULL COMMENT '主键',
    `tenant_code`  VARCHAR(64)  NOT NULL COMMENT '租户编码',
    `code`         VARCHAR(64)  NOT NULL COMMENT '模板编码',
    `name`         VARCHAR(128) NOT NULL COMMENT '模板名称',
    `version`      VARCHAR(16)  NULL COMMENT '版本',
    `biz_type`     VARCHAR(64)  NOT NULL COMMENT '业务类型（引擎透明）',
    `nodes_json`   TEXT         NULL COMMENT '模板节点 JSON（宿主语义）',
    `published_at` TIMESTAMP(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
    `published_by` VARCHAR(64)  NULL,
    `is_deleted`   INT          NOT NULL DEFAULT 0,
    `create_by`    VARCHAR(64)  NULL,
    `update_by`    VARCHAR(64)  NULL,
    `create_time`  TIMESTAMP(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time`  TIMESTAMP(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_z_ctc_approval_tpl` (`tenant_code`, `code`, `is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `z_ctc_approval_ticket`
(
    `id`                    VARCHAR(64)   NOT NULL COMMENT '主键',
    `tenant_code`           VARCHAR(64)   NOT NULL,
    `code`                  VARCHAR(64)   NOT NULL COMMENT '票编码',
    `title`                 VARCHAR(256)  NULL,
    `template_code`         VARCHAR(64)   NULL,
    `template_name`         VARCHAR(128)  NULL,
    `biz_type`              VARCHAR(64)   NOT NULL,
    `biz_ref`               VARCHAR(128)  NULL COMMENT '业务引用（引擎透明）',
    `biz_name`              VARCHAR(256)  NULL,
    `biz_role`              VARCHAR(64)   NULL,
    `applicant_id`          VARCHAR(64)   NULL,
    `applicant_name`        VARCHAR(128)  NULL,
    `resource_codes_json`   TEXT          NULL,
    `status`                VARCHAR(32)   NOT NULL COMMENT 'PENDING/APPROVED/REJECTED/CANCELED',
    `current_node`          INT           NULL DEFAULT 0,
    `current_approver_name` VARCHAR(512)  NULL,
    `reason`                VARCHAR(1024) NULL,
    `nodes_json`            TEXT          NULL COMMENT '节点展开快照（宿主展开的审批人集合）',
    `finished_at`           TIMESTAMP(3)  NULL DEFAULT NULL,
    `is_deleted`            INT           NOT NULL DEFAULT 0,
    `create_by`             VARCHAR(64)   NULL,
    `update_by`             VARCHAR(64)   NULL,
    `create_time`           TIMESTAMP(3)  NULL DEFAULT CURRENT_TIMESTAMP(3),
    `update_time`           TIMESTAMP(3)  NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_z_ctc_approval_tk_page` (`tenant_code`, `status`, `create_time`),
    KEY `idx_z_ctc_approval_tk_biz` (`tenant_code`, `biz_type`, `biz_ref`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
