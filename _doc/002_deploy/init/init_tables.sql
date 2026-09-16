-- 租户表
CREATE TABLE IF NOT EXISTS z_ctc_tenant
(
    id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,
    tenant_code
    VARCHAR
(
    50
) NOT NULL UNIQUE COMMENT '租户编码',
    tenant_name VARCHAR
(
    100
) NOT NULL COMMENT '租户名称',
    contact_name VARCHAR
(
    50
) COMMENT '联系人',
    contact_phone VARCHAR
(
    20
) COMMENT '联系电话',
    contact_email VARCHAR
(
    100
) COMMENT '联系邮箱',
    status INT DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
    expire_time DATETIME COMMENT '过期时间',
    gmt_create DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_tenant_code
(
    tenant_code
)
    ) COMMENT '租户表';

-- 域表
CREATE TABLE IF NOT EXISTS z_ctc_domain
(
    id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,
    domain_code
    VARCHAR
(
    50
) NOT NULL UNIQUE COMMENT '域编码',
    domain_name VARCHAR
(
    100
) NOT NULL COMMENT '域名称',
    tenant_id BIGINT COMMENT '租户ID',
    status INT DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
    description VARCHAR
(
    500
) COMMENT '描述',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_tenant_id
(
    tenant_id
),
    INDEX idx_domain_code
(
    domain_code
)
    ) COMMENT '域表';

-- 组织表
CREATE TABLE IF NOT EXISTS z_ctc_org
(
    id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,
    org_name
    VARCHAR
(
    100
) NOT NULL COMMENT '组织名称',
    tenant_id BIGINT COMMENT '租户ID',
    domain_id BIGINT COMMENT '域ID',
    parent_id BIGINT DEFAULT 0 COMMENT '上级组织ID',
    status INT DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
    description VARCHAR
(
    500
) COMMENT '描述',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_tenant_id
(
    tenant_id
),
    INDEX idx_domain_id
(
    domain_id
)
    ) COMMENT '组织表';

-- 部门表
CREATE TABLE IF NOT EXISTS z_ctc_dept
(
    id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,
    dept_name
    VARCHAR
(
    100
) NOT NULL COMMENT '部门名称',
    tenant_id BIGINT COMMENT '租户ID',
    domain_id BIGINT COMMENT '域ID',
    org_id BIGINT COMMENT '组织ID',
    parent_id BIGINT DEFAULT 0 COMMENT '上级部门ID',
    status INT DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
    description VARCHAR
(
    500
) COMMENT '描述',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_tenant_id
(
    tenant_id
),
    INDEX idx_domain_id
(
    domain_id
),
    INDEX idx_org_id
(
    org_id
)
    ) COMMENT '部门表';

-- 组表
CREATE TABLE IF NOT EXISTS z_ctc_group
(
    id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,
    group_name
    VARCHAR
(
    100
) NOT NULL COMMENT '组名称',
    tenant_id BIGINT COMMENT '租户ID',
    domain_id BIGINT COMMENT '域ID',
    org_id BIGINT COMMENT '组织ID',
    dept_id BIGINT COMMENT '部门ID',
    status INT DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
    description VARCHAR
(
    500
) COMMENT '描述',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_tenant_id
(
    tenant_id
),
    INDEX idx_domain_id
(
    domain_id
),
    INDEX idx_org_id
(
    org_id
),
    INDEX idx_dept_id
(
    dept_id
)
    ) COMMENT '组表';

-- 插入测试数据
INSERT INTO z_ctc_tenant (tenant_code, tenant_name, contact_name, contact_phone, status)
VALUES ('DEFAULT', '默认租户', '管理员', '13800138000', 1);

INSERT INTO z_ctc_domain (domain_code, domain_name, tenant_id, status)
VALUES ('DEFAULT', '默认域', 1, 1);
