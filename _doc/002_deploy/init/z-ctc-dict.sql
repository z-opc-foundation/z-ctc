-- =============================================================================
-- z-ctc-dict 灵活字典域 SQL (MySQL 8.0 + Druid)
--
-- 2 张表: 字典分类 / 字典条目.
-- 表名沿用 F040 迁移 (z-opc/_doc/004_sql/z-dict-migration.sql) 的既有形状:
--   z_dict_type  -> z_ctc_dict_category
--   z_dict_item  -> z_ctc_dictionary
-- 灵活度四层:
--   结构: 分类可成树 (parent_code); 条目 flat/tree 两用 (parent_key)
--   值态: value_type(string/number/boolean/json) + data_value + ext_props(ext_schema 约定)
--   作用域: GLOBAL/TENANT/DOMAIN 三级, 读取回退链 domain -> tenant -> global (高层覆盖低层)
--   接入: builtin 内置标记 / tags+icon+color 渲染提示 / ref_category+ref_key 级联 / 生效窗口
-- 作用域列约定: 空串 = 全局/继承, 不用 NULL (MySQL 唯一索引对 NULL 不去重).
-- =============================================================================

-- 字典分类 (字典类型)
CREATE TABLE IF NOT EXISTS `z_ctc_dict_category`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `cat_code`    VARCHAR(64)  NOT NULL COMMENT '分类编码 (业务引用键, 如 user_status)',
    `cat_name`    VARCHAR(128) NOT NULL COMMENT '分类显示名',
    `parent_code` VARCHAR(64)  NULL COMMENT '父分类编码 (分类自身可成树, NULL=顶层)',
    `scope`       VARCHAR(16)  NOT NULL DEFAULT 'TENANT' COMMENT '作用域 GLOBAL/TENANT/DOMAIN',
    `tenant_code` VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '作用域租户 (空=全局)',
    `domain_code` VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '作用域域 (空=继承租户)',
    `value_type`  VARCHAR(16)  NOT NULL DEFAULT 'string' COMMENT '条目值类型 string/number/boolean/json',
    `entry_shape` VARCHAR(16)  NOT NULL DEFAULT 'flat' COMMENT '条目形状 flat/tree',
    `ext_schema`  TEXT         NULL COMMENT '条目扩展属性 schema (JSON: 字段名->类型/标签/必填)',
    `builtin`     TINYINT      NOT NULL DEFAULT 0 COMMENT '系统内置 (1=代码登记枚举, 禁改编码禁删)',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 (1=启用 0=禁用)',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `description` VARCHAR(500) NULL COMMENT '描述',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by`  VARCHAR(64)  NULL COMMENT '创建者',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by`  VARCHAR(64)  NULL COMMENT '更新者',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cat_scope` (`cat_code`, `tenant_code`, `domain_code`),
    KEY `idx_cat_tenant` (`tenant_code`),
    KEY `idx_cat_parent` (`parent_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='字典分类 (灵活字典)';

-- 字典条目
CREATE TABLE IF NOT EXISTS `z_ctc_dictionary`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `category`       VARCHAR(64)  NOT NULL COMMENT '所属分类编码 (z_ctc_dict_category.cat_code)',
    `dict_key`       VARCHAR(64)  NOT NULL COMMENT '条目编码 (分类作用域内唯一)',
    `dict_value`     VARCHAR(255) NOT NULL COMMENT '显示文本 (label)',
    `data_value`     VARCHAR(512) NULL COMMENT '数据值 (形态由分类 value_type 决定, NULL=同 dict_key)',
    `parent_key`     VARCHAR(64)  NULL COMMENT '父条目 dict_key (tree 形状时组树, NULL=顶层)',
    `tenant_code`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '作用域租户 (空=全局)',
    `domain_code`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '作用域域 (空=继承租户)',
    `ext_props`      TEXT         NULL COMMENT '扩展属性 JSON (结构由分类 ext_schema 约定)',
    `tags`           VARCHAR(255) NULL COMMENT '标签 (逗号分隔, 别名/搜索用)',
    `icon`           VARCHAR(64)  NULL COMMENT '图标名 (前端渲染提示)',
    `color`          VARCHAR(16)  NULL COMMENT '颜色 (antd 语义色或色值, 前端渲染提示)',
    `ref_category`   VARCHAR(64)  NULL COMMENT '级联引用的分类编码 (与 ref_key 成对)',
    `ref_key`        VARCHAR(64)  NULL COMMENT '级联引用的条目 dict_key (与 ref_category 成对)',
    `effective_from` DATETIME     NULL COMMENT '生效起 (NULL=不限)',
    `effective_to`   DATETIME     NULL COMMENT '生效止 (NULL=不限)',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 (1=启用 0=禁用)',
    `sort_order`     INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `description`    VARCHAR(500) NULL COMMENT '描述',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `created_by`     VARCHAR(64)  NULL COMMENT '创建者',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by`     VARCHAR(64)  NULL COMMENT '更新者',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_entry_scope` (`category`, `dict_key`, `tenant_code`, `domain_code`),
    KEY `idx_entry_cat` (`category`),
    KEY `idx_entry_parent` (`parent_key`),
    KEY `idx_entry_tenant` (`tenant_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='字典条目 (灵活字典)';
