# z-ctc 4A 中心自闭环改造方案

> **任务**：参考行业 4A 中心通用能力模型，反推 z-ctc 哪些功能欠缺，输出改造方案。
> **目标**：z-ctc 功能自闭环 / 可独立 / 很完善，**0 私域 groupId 依赖**。
> **调研时间**：2026-06-16
> **调研范围**：
> - 主流 4A 中心产品的能力模型（sso/ac/spc/osc/surl/audit 6 域）
> - 主流 4A 中心通常包含 6 大域：认证（sso）/ 账号（ac）/ 授权（spc）/ 业务对象（osc）/ 资源短链（surl）/ 审计（audit）
> - z-ctc — 180+ java 文件，8 子模块（z-ctc-core / z-ctc-web / z-ctc-admin / z-ctc-client / z-ctc-sdk / z-ctc-sso /
    z-ctc-audit-starter / bootstrap）

---

## 1. 摘要

### 1.1 现状确认

| 维度             | 状态                                                                  |
|----------------|---------------------------------------------------------------------|
| 私域 groupId 依赖  | **0**（z-ctc 所有 pom + java + config 全文 grep 无任何**非公开 groupId 前缀**引用） |
| 自闭环程度          | **依赖图闭环**（仅依赖 z-boot / z-util / z-meta / z-ctc 内部），但**功能未闭环**       |
| 4A 中心能力        | **覆盖 2/6 域**（sso + audit），**缺失 4/6 域**（ac / spc / osc / surl）       |
| 与 主流 4A 中心体量差距 | 2000+ 文件 vs z-ctc 180+ 文件 = **10x+**                                |

**核心矛盾**：z-ctc 名字叫"4A 中心"（Authentication / Authorization / Account / Audit），但实际只覆盖了 A（认证）和 Audit 两个
A。

### 1.2 差距一句话

z-ctc 现在是 **"登录服务 + 一点审计"**，离真正的 4A 中心还差：

- **A**ccount（账号全生命周期管理）
- **A**uthorization（细粒度权限：菜单/资源/数据/角色/单位/产品/应用/属性）
- **O**SC（业务对象：组织/人员/岗位/标签/异动/兼职 — 大型 HR/组织架构能力，**决策：建议简化抛弃**）
- **S**URL（短网址映射：URL 隐藏 + 跳转追踪）

---

## 2. 行业 4A 中心 6 域能力全景

主流 4A 中心产品按"client（远端 RPC 封装）+ core（实现）+ starter（Controller）"分层，6 域 = **5 业务域 + 1 横切**：

### 2.1 域映射表

| 域                       | 子模块                                           | 主要职责                                    | SQL 表                                           | 关键类                                       |
|-------------------------|-----------------------------------------------|-----------------------------------------|-------------------------------------------------|-------------------------------------------|
| **Authentication**      | sso-client (大量 java)                          | SSO 单点登录、Token 签发/校验、SM2 加密             | sso_*                                           | sso.client.config / annotation / utils    |
| **Account**             | (在 ac/table 下 4 张表)                           | 账号 + 凭证（手机/邮箱/用户名）+ 登录日志                | ac_account / ac_auth / ac_login_log / ac_tenant | —                                         |
| **Authorization (spc)** | spc-client (大量 java) + core/spc               | 角色 / 资源 / 权限 / 产品 / 应用 / 菜单 / 单位授权 / 属性 | 20 张 spc_*                                      | spc.rpc.client.{dto.req,rsp,service}      |
| **OSC（业务对象）**           | osc-client (大量 java) + sdk/osc                | 组织 / 人员 / 岗位 / 标签 / 资质 / 异动 / 兼职        | 23 张 osc_*                                      | osc.client.api.service / request.unit.cmd |
| **SURL（资源权限）**          | surl-client (大量 java) + core/surl             | 短网址映射（map_key ↔ origin_url）             | surl_mapping (1 张)                              | surl.client (极少代码)                        |
| **Audit**               | (在 spc_audit_log / spc_op_log + common/audit) | 审计 + 操作日志                               | 2 张 spc_audit + spc_op_log                      | audit.starter                             |

### 2.2 4A 中心 6 域依赖图

```
common (公共: 钉钉/微信/审计/工具)
   ├── dingding (企业钉钉集成)
   ├── weixin (微信集成)
   ├── gitlab (GitLab 集成)
   └── audit (审计基础)

sso-client ─┐
spc-client ─┤
osc-client ─┼─→ core (核心实现: Mapper + Service)
surl-client ─┘
                ↓
starter (Spring Boot 启动: Controller + Web 配置)
                ↓
sdk (osc 专用 SDK)
```

**z-ctc 应该采用的类似分层**：

```
z-ctc-common (公共: 工具 + 异常 + 常量)
   ├── utils (密码 / 加密 / Token)
   └── exception (业务异常)

z-ctc-sso ─┐
z-ctc-authz (新: spc + ac) ─┤
z-ctc-org (新: osc 简化版)  ─┼─→ z-ctc-core (核心实现)
z-ctc-surl (新)              ─┘
                            ↓
z-ctc-web + z-ctc-admin (Controller)
                            ↓
z-ctc-sdk (对外 SPI + 客户端)
                            ↓
z-ctc-audit-starter (横切: 审计)
                            ↓
z-ctc-client (统一 client facade)
                            ↓
z-ctc-spring-boot-starter (聚合 starter, 复用 z-ctc-web)
```

---

## 3. z-ctc 现状能力矩阵

### 3.1 子模块现状

| 子模块                 | 文件数  | 主要内容                                         | 状态             |
|---------------------|------|----------------------------------------------|----------------|
| z-ctc-core          | ~140 | domain.entity / service.impl / model.req,rsp | **核心**（实现散落各域） |
| z-ctc-web           | ~25  | web.api.request,response                     | **API 层**      |
| z-ctc-admin         | ~3   | exception/（**基本空**）                          | **占位**         |
| z-ctc-client        | 0    | **完全空**                                      | **占位**         |
| z-ctc-sdk           | 0    | **完全空**                                      | **占位**         |
| z-ctc-sso           | ~6   | sso.config + sso.model                       | **认证**         |
| z-ctc-audit-starter | ~8   | audit.starter                                | **审计 starter** |
| bootstrap           | 0    | **空**                                        | **占位**         |

### 3.2 4A 6 域能力对比

| 4A 域               | 主流 4A 中心产品                         | z-ctc 现状            | 状态          | 改造量    |
|--------------------|------------------------------------|---------------------|-------------|--------|
| **A**uthentication | sso-client (大量 java)               | z-ctc-sso (6)       | **基础有，需加固** | 中      |
| **A**ccount        | ac/table 4 张                       | （散在 z-ctc-core）     | **未明确边界**   | 大      |
| **A**uthorization  | spc-client (大量 java) + core (400+) | （散在 z-ctc-core）     | **未明确边界**   | **大**  |
| **O**SC 业务对象       | osc-client (大量 java) + core        | **完全无**             | **缺失**      | **决策** |
| **S**URL 资源权限      | surl-client (1) + core             | **完全无**             | **缺失**      | 小      |
| **A**udit          | spc_audit_log + spc_op_log         | z-ctc-audit-starter | **基础有，需加固** | 中      |

---

## 4. 改造方案（按域 + 纳入/抛弃决策）

### 4.1 Authentication（认证）— **纳入，扩展**

**现状**：z-ctc-sso 有 RemoteTokenService（TokenService 接口），但用了 z-util-http 1.0.2-SNAPSHOT 中**实际不存在**的
HttpExecutor API（同 z-lc-core Adapter 历史错配问题，**不是私库问题**）。

**4A 中心能力清单**（参考行业 4A 中心-sso-client 100+ java）：

- [ ] 单点登录（SSO）ticket 签发 / 校验
- [ ] 多端登录（Web / App / 小程序）
- [ ] Token 刷新 / 过期策略
- [ ] 多种登录验证方式（密码 / 短信 / 扫码 / 钉钉 / 微信 / OAuth）
- [ ] 临时账号（admin 后台代开）
- [ ] SM2 国密加密传输 / 存储（主流 4A 中心产品 有 5 个 doc 详细说明）
- [ ] 密码强度校验（主流 4A 中心产品 有 puml 流程图）
- [ ] 登录失败锁定 / 防爆破
- [ ] 账号自助注册流程
- [ ] 登录日志（主流 4A 中心产品 ac_login_log 表）

**z-ctc 改造建议**：
| 能力 | 决策 | 理由 |
|------|------|------|
| SSO Ticket | **纳入** | 已有 z-ctc-sso，扩展 |
| 多端登录 | **纳入** | 一人公司多端必备 |
| Token 刷新 | **纳入** | 安全基础 |
| 多种登录方式（密码 / 短信 / OAuth）| **纳入** | 一人公司场景够用 |
| 扫码 / 钉钉 / 微信 | **暂不纳入** | 一人公司用不到（z-ctc 不应该绑定钉钉/微信）|
| 临时账号 | **纳入** | 简单可实现 |
| SM2 国密 | **决策待定** | 金融/政企需要，普通场景不需要 |
| 密码强度 | **纳入** | 安全基础 |
| 登录失败锁定 | **纳入** | 安全基础 |
| 登录日志 | **纳入**（落到 ac_login_log 表）| 审计基础 |

**改造 SQL（z-ctc.sql 新增）**：

```sql
-- 登录日志（主流 4A 中心产品 ac_login_log）
z_ctc_login_log (
  id, tenant_code, account_id, account_no,
  login_type, login_ip, user_agent,
  login_status, fail_reason,
  create_time, is_deleted
)
```

---

### 4.2 Account（账号）— **纳入，扩展**

**现状**：z-ctc-core 已有 user / account 相关的 DOs 和 Service，但**没有 ac_* 4 张表的明确边界**。

**4A 中心能力清单**（参考 ac/table 4 张表 + ac_* Service）：

- [ ] ac_account（账户：账号/密码/状态/类型/历史密码/租户）
- [ ] ac_auth（凭证：手机/邮箱/用户名/密码/Salt/历史凭证/密码强度）
- [ ] ac_login_log（登录日志：见 4.1）
- [ ] ac_tenant（租户：多租户隔离）

**z-ctc 改造建议**：
| 能力 | 决策 | 理由 |
|------|------|------|
| 多租户（ac_tenant）| **纳入** | z-opc 多模块已用 `tenant_code` 字段，z-ctc 必须支持 |
| ac_account + ac_auth 拆分 | **纳入** | 一个用户可绑多凭证（手机 + 邮箱），拆分更合理 |
| 账号类型（超级管理员 / 租户管理员 / 普通成员）| **纳入** | 主流 4A 中心产品 3 类（type: 1/2/3）|
| 历史密码 | **纳入** | 安全合规 |
| 临时账号（主流 4A 中心产品 puml "临时账号.puml"）| **纳入** | admin 后台代开 |
| 员工批量修改（主流 4A 中心产品 puml）| **纳入** | admin 后台常用 |
| 账号自助注册（主流 4A 中心产品 puml）| **纳入** | 对外开放场景 |

**改造 SQL**：

```sql
-- 账户（主流 4A 中心产品 ac_account）
z_ctc_account (
  id, tenant_code, account_no, password,
  history_password, register_time, last_modify_password_time,
  account_status, type, extra,
  create_time, update_time, is_deleted
)

-- 凭证（主流 4A 中心产品 ac_auth）
z_ctc_auth (
  id, tenant_code, identifier, identity_type, credential,
  account_id, history_credential, salt, new_credential,
  credential_strength, last_modify_credential_time,
  create_time, update_time, is_deleted
)

-- 租户（主流 4A 中心产品 ac_tenant）
z_ctc_tenant (
  id, tenant_code, tenant_name, status,
  expire_time, config_json,
  create_time, update_time, is_deleted
)
```

---

### 4.3 Authorization（授权 / spc）— **纳入，核心改造**

**现状**：z-ctc-core 散落 role / permission 相关 DOs 和 Service，**没有统一 spc 域**。

**4A 中心能力清单**（参考 spc/table 20 张表 + spc-client 200+ java）：

| 表名                                     | 能力                  | 决策                  | 理由            |
|----------------------------------------|---------------------|---------------------|---------------|
| spc_role                               | 角色（编码/名称/父角色/部门/机构） | **纳入**              | RBAC 基础       |
| spc_permission                         | 权限（按钮/接口/数据范围）      | **纳入**              | 细粒度           |
| spc_role_permission_relation           | 角色-权限关联             | **纳入**              | 关联表           |
| spc_menu                               | 菜单（树形）              | **纳入**              | 前端路由          |
| spc_resource                           | 资源（1=功能权限 2=数据权限）   | **纳入**              | 主流 4A 中心产品 核心 |
| spc_resource_attr                      | 资源属性                | **纳入**              | 资源动态配置        |
| spc_resource_attr_value                | 资源属性值               | **纳入**              | 同上            |
| spc_resource_attr_value_grant_relation | 属性值授权               | **纳入**              | 动态授权          |
| spc_resource_operate                   | 资源操作（增删改查等）         | **纳入**              | 按钮级           |
| spc_subject_attr                       | 主体属性                | **纳入**              | 用户/部门属性化      |
| spc_subject_grant_relation             | 主体授权                | **纳入**              | 灵活授权          |
| spc_unit_permission                    | 单位权限                | **纳入**（简化）          | 一人公司一般单一单位    |
| spc_application_info                   | 应用信息                | **纳入**              | 多应用           |
| spc_product_info                       | 产品信息                | **纳入**              | 多产品           |
| spc_product_app                        | 产品-应用               | **纳入**              | 关联            |
| spc_product_app_map                    | 产品-应用映射             | **纳入**              | 同上            |
| spc_role_show_tree                     | 角色显示树               | **纳入**              | 前端树           |
| spc_audit_log                          | 审计日志                | **合并到 z-ctc-audit** | 域内统一          |
| spc_op_log                             | 操作日志                | **合并到 z-ctc-audit** | 域内统一          |

**核心抽象**（spc-client 的 DTO 设计）：

- `PermissionDTO`（资源 + 操作 + 数据范围 + 表达式）
- `SubjectDTO`（主体：用户 / 部门 / 角色）
- `GrantDTO`（授权关系）
- `CheckPermissionRequest/Rsp`

**z-ctc 改造建议**：

1. **新建 z-ctc-authz 子模块**（参考行业 4A 中心-spc-client 拆分）
    - `z-ctc-authz-common`：DTO + 枚举 + 常量
    - `z-ctc-authz-core`：核心 Service + Mapper
    - `z-ctc-authz-web`：Controller
2. **spc_role + spc_permission 抽通用 RBAC**（最简 RBAC: user-role-permission 三表）
3. **数据权限（主流 4A 中心产品 资源类型=2）**用 SpEL 表达式实现（不引入 QLExpress，避免新依赖）

**主流 4A 中心产品 RBAC 之外的关键能力（决策）**：
| 能力 | 决策 | 理由 |
|------|------|------|
| 资源属性动态配置 | **纳入（简化）** | 必备，存 JSON |
| 数据权限（行级 / 列级）| **纳入** | 细粒度 |
| 按钮级权限 | **纳入** | 必备 |
| 临时授权 / 委派 | **纳入** | 必备 |
| ABAC（属性驱动）| **暂不纳入** | 复杂度高，一期用 RBAC 够 |
| 单位权限（spc_unit_permission）| **简化** | 一人公司单一单位场景 |
| 复杂表达式（QLExpress）| **不纳入** | SpEL 足够，QLExpress 引入太重 |

---

### 4.4 OSC（业务对象 / 组织架构）— **决策：抛弃大部分，保留核心**

**现状**：z-ctc 完全无 OSC 能力。

**4A 中心能力清单**（参考 osc/table 23 张表 + osc-client 200+ java）：

| 表名                                           | 能力          | 决策         | 理由         |
|----------------------------------------------|-------------|------------|------------|
| osc_unit                                     | 组织/部门       | **简化纳入**   | 必备         |
| osc_unit_relation                            | 部门关系        | **简化纳入**   | 树形         |
| osc_unit_relation_config                     | 关系配置        | **简化纳入**   | 同上         |
| osc_person                                   | 自然人         | **纳入**     | 区分"人"和"账号" |
| osc_staff                                    | 员工（人+岗位+单位） | **纳入**     | HR 基础      |
| osc_staff_position                           | 员工-岗位       | **纳入**     | 多岗位        |
| osc_staff_unit                               | 员工-单位       | **纳入**     | 跨部门        |
| osc_staff_unit_relation_config               | 员工-单位关系配置   | **纳入**     | 兼职         |
| osc_position                                 | 岗位          | **纳入**     | HR 基础      |
| osc_job_role                                 | 职位角色        | **纳入**     | 关联权限       |
| osc_location                                 | 工作地点        | **简化纳入**   | 可选         |
| osc_manage_tree                              | 管理树         | **纳入**     | 审批链        |
| osc_qualification / osc_person_qualification | 资质          | **不纳入**    | 一人公司无资质管理  |
| osc_tag / osc_tag_category                   | 标签          | **纳入（通用）** | 灵活分类       |
| osc_object_tag                               | 对象标签        | **纳入**     | 同上         |
| osc_change_plan / osc_change_plan_detail     | 异动计划        | **不纳入**    | 复杂 HR 流程   |
| osc_change_record                            | 异动记录        | **不纳入**    | 同上         |
| osc_irregular_account_apply                  | 非常规账号申请     | **不纳入**    | 走 admin 流程 |
| osc_loc_occupation                           | 地点占用        | **不纳入**    | 工位管理       |
| osc_entity_attr / osc_entity_attr_value      | 实体属性        | **纳入（通用）** | 业务对象可配置属性  |
| osc_sdk                                      | 业务对象 SDK    | **纳入（轻量）** | 业务侧扩展      |

**核心决策**：

- OSC 是 **HR/组织架构 + 业务对象** 混合体
- 一人公司场景只需要：
    1. **部门/组织**（单位 + 关系）— 必备
    2. **员工 / 岗位**（基础）— 必备
    3. **管理树**（审批链）— 必备（与 z-camuda 协同）
    4. **标签**（通用）— 必备
    5. **实体属性**（动态配置）— 选做
- 抛弃：
    - 异动计划 / 异动记录（复杂 HR 流程）
    - 资质管理
    - 非常规账号申请
    - 地点 / 工位

**z-ctc 改造建议**：

- **不新建 z-ctc-org 子模块**（避免过度工程化）
- 在 z-ctc-core 加 `org/` 包：`OrgUnit / OrgStaff / OrgPosition / OrgTag` DOs + Service
- 主流 4A 中心产品 23 张表 → **z-ctc 5-6 张表**（unit / staff / position / staff_position / staff_unit / manage_tree）

---

### 4.5 SURL（短网址 / 资源权限）— **纳入，轻量实现**

**现状**：z-ctc 完全无 SURL 能力。

**4A 中心能力清单**（参考 surl/table 1 张表 + surl-client 1 java）：

- 短网址生成：map_key（短码）↔ origin_url（原始 URL）
- map_key 唯一约束，origin_url 哈希索引
- 用途：链接缩短、跳转追踪、URL 隐藏

**z-ctc 改造建议**：

- **新建 z-ctc-surl 子模块**（**轻量**，参考行业 4A 中心 1 个 java 即可）
- `z-ctc-surl-core`：SurMapping DO + Mapper + Service（生成短码、解析跳转）
- `z-ctc-surl-web`：Controller（创建 / 查询 / 删除）
- 短码算法：用 Snowflake（避免依赖），不用 hash（避免冲突）

**SQL**：

```sql
z_ctc_surl_mapping (
  id BIGINT PK,
  map_key VARCHAR(16) UNIQUE,
  origin_url VARCHAR(2048),
  origin_url_hash VARCHAR(16),
  expired_at DATETIME,
  create_time, update_time, is_deleted
)
```

**决策**：**完全纳入**（实现简单，1 张表 + 1 个子模块，价值大：可作"邀请链接 / 内部跳转"通用能力）。

---

### 4.6 Audit（审计）— **纳入，扩展**

**现状**：z-ctc-audit-starter 有基础结构，但**功能薄**。

**4A 中心能力清单**（参考 spc_audit_log / spc_op_log + common/audit）：

- 审计日志（spc_audit_log）：登录 / 登出 / 关键操作
- 操作日志（spc_op_log）：CRUD / 数据修改 / 状态变更
- 异步落库（不阻塞业务）
- 上下文注入（操作人 / IP / 浏览器 / 租户 / TraceId）

**z-ctc 改造建议**：
| 能力 | 决策 | 理由 |
|------|------|------|
| 审计日志 | **纳入**（合并 主流 4A 中心产品 spc_audit_log）| 关键操作留痕 |
| 操作日志 | **纳入**（合并 主流 4A 中心产品 spc_op_log）| 数据变更追踪 |
| 异步落库 | **纳入** | MQ/线程池异步 |
| 上下文注入 | **纳入** | 通过 ThreadLocal + Interceptor |
| 审计查询 UI | **纳入**（z-ctc-admin）| 后台查看 |
| 审计导出 | **纳入** | 合规 |
| 数据脱敏 | **纳入** | 密码/身份证/手机号 |
| 审计告警 | **暂不纳入** | 复杂规则 |

**SQL**：

```sql
z_ctc_audit_log (
  id, tenant_code, operator_id, operator_name,
  module, action, target_type, target_id,
  request_payload, response_payload,
  ip, user_agent, trace_id, cost_ms,
  result, error_msg,
  create_time
)

z_ctc_op_log (
  id, tenant_code, operator_id, table_name, record_id,
  operation_type (INSERT/UPDATE/DELETE),
  before_data, after_data,
  ip, create_time
)
```

---

## 5. 改造后的 z-ctc 子模块结构

```
z-ctc/
├── pom.xml                                  # parent
├── z-ctc-common/                            # 新建: 公共 DTO + 异常 + 工具 + 常量
├── z-ctc-core/                              # 已有: domain + service（按域拆包: authn / authz / account / org / audit）
├── z-ctc-sso/                               # 已有: 扩展为完整 Authentication
├── z-ctc-authz/                             # 新建: Authorization 独立子模块（spc 等价）
│   ├── z-ctc-authz-common/                  # Permission / Role / Resource DTO
│   ├── z-ctc-authz-core/                    # Mapper + Service
│   └── z-ctc-authz-web/                     # Controller
├── z-ctc-surl/                              # 新建: 短网址子模块
│   ├── z-ctc-surl-core/
│   └── z-ctc-surl-web/
├── z-ctc-web/                               # 已有: 聚合 Web 入口
├── z-ctc-admin/                             # 已有: 完善 (admin 后台)
├── z-ctc-client/                            # 已有（空）: 客户端 facade
├── z-ctc-sdk/                               # 已有（空）: 对外 SPI
├── z-ctc-audit-starter/                     # 已有: 扩展为完整 Audit
├── z-ctc-spring-boot-starter/               # 新建: 聚合 starter (复用 z-ctc-web 模式)
├── _frontend/                               # 已有
├── _frontend_component/                     # 已有
└── _doc/
    ├── 4A-改造方案.md                       # 本文档
    ├── spc-data-model.md                    # spc 数据模型详细设计（待补）
    ├── authentication-flow.md               # 认证流程图（待补）
    └── api-contract.md                      # API 契约（待补）
```

**子模块数**：8 → **12**（新增 4 个：z-ctc-common / z-ctc-authz × 3 / z-ctc-surl × 2 / z-ctc-spring-boot-starter）

---

## 6. 自闭环检查清单

### 6.1 依赖图

- [ ] **0 私域 groupId 依赖**（任何非公开 groupId 前缀 — 自检时排除 org.* / com.* / io.* / 顶级开源项目）
- [ ] **0 hmos / hdos / hwc 依赖**
- [ ] 0 kingbase8 / dm / vb 数据库驱动
- [ ] 仅依赖 z-boot / z-util / z-meta / z-config / Spring Boot 公共库 / alibaba common（druid / easyexcel / QLExpress
  视情况）

### 6.2 4A 6 域全部覆盖

- [ ] **Authentication**：SSO / 多端登录 / 多种登录方式 / 临时账号 / 密码强度 / 登录日志
- [ ] **Account**：ac_account / ac_auth / ac_tenant / 账号类型 / 历史凭证
- [ ] **Authorization**：RBAC + 资源 + 数据权限 + 按钮权限 + 临时授权
- [ ] **OSC**：org unit + staff + position + manage_tree + tag（**简化版**）
- [ ] **SURL**：短网址生成 + 解析 + 过期
- [ ] **Audit**：audit_log + op_log + 上下文注入 + 异步落库

### 6.3 工程标准

- [ ] Swagger 1.x 注解（z-opc 约定）— z-ctc 现有 swagger 1.x 风格保留
- [ ] Knife4j 1.x starter（z-ctc-admin 现有）
- [ ] MyBatis-Plus 3.3.1 + Druid 1.2.18（z-opc 约定）
- [ ] JWT 走 JJWT 0.11.5（z-ctc-audit-starter 已有）
- [ ] 密码加密：BCrypt（z-ctc-core 已有 spring-security-crypto）
- [ ] 国密 SM2：**可选**（z-ctc-core 不强制依赖）

### 6.4 独立运行

- [ ] z-ctc 单独启服务能跑（z-ctc-spring-boot-starter 聚合）
- [ ] 不依赖 z-config / z-meta / z-camuda 等其他 z-opc 模块
- [ ] 0 业务模块耦合（z-ctc 只暴露 SPI，**被**依赖，不依赖别人）

### 6.5 测试

- [ ] 单元测试覆盖率 > 60%（按 z-opc 现有标准）
- [ ] 关键流程有集成测试：登录 / 授权校验 / 审计查询
- [ ] Smoke test 脚本（curl / postman）

---

## 7. 实施路线图

### Phase A：基础加固（1 周）

**目标**：Authentication + Audit 加固，独立可跑

| 任务                                                                             | 工作量      |
|--------------------------------------------------------------------------------|----------|
| A1. 修 z-ctc-sso z-util-http 中不存在的 HttpExecutor API（用 JDK HttpURLConnection 替代） | 0.5 天    |
| A2. z-ctc-sso 扩展：多端登录 / Token 刷新 / 临时账号                                        | 1.5 天    |
| A3. z-ctc-audit-starter 扩展：spc_audit_log + spc_op_log 合并 / 异步落库 / 上下文注入        | 2 天      |
| A4. z-ctc-spring-boot-starter 聚合（让 z-ctc 可独立跑）                                 | 0.5 天    |
| A5. z-ctc.sql 增量 + migration 脚本                                                | 0.5 天    |
| A6. 单元测试 + 集成测试                                                                | 1 天      |
| A7. 端到端冒烟（启动 + curl 验证）                                                        | 0.5 天    |
| **小计**                                                                         | **~1 周** |

**交付**：z-ctc 独立可启（z-ctc-spring-boot-starter 跑得起来），4A 中心 2/6 域（Authentication + Audit）完整。

### Phase B：Account + Authorization（2-3 周）

**目标**：4A 4/6 域（加 Account + Authorization）

| 任务                                                                                                                                    | 工作量        |
|---------------------------------------------------------------------------------------------------------------------------------------|------------|
| B1. 新建 z-ctc-common 子模块（公共 DTO / 异常 / 工具 / 常量）                                                                                        | 1 天        |
| B2. 拆分 z-ctc-core account 包（ac_account / ac_auth / ac_tenant 3 张表 + Service）                                                          | 2 天        |
| B3. 新建 z-ctc-authz 子模块（common / core / web）                                                                                           | 1 天        |
| B4. 蒸馏参考实现 spc 表 → z-ctc SQL（spc_role / spc_permission / spc_menu / spc_resource / spc_resource_attr / spc_resource_operate + 5 张关联表） | 1 天        |
| B5. spc DTO 蒸馏（Permission / Role / Resource / Subject / Grant DTO）                                                                    | 2 天        |
| B6. RBAC Service 实现（user-role-permission 三表核心 + 资源 + 数据权限 SpEL 表达式）                                                                   | 3 天        |
| B7. z-ctc-web Controller 扩展（认证 / 账号 / 授权 API）                                                                                         | 2 天        |
| B8. z-ctc-admin 后台扩展（用户管理 / 角色管理 / 权限分配）                                                                                              | 2 天        |
| B9. 单元测试 + 集成测试                                                                                                                       | 2 天        |
| B10. 端到端冒烟                                                                                                                            | 1 天        |
| **小计**                                                                                                                                | **~2-3 周** |

**交付**：4A 中心 4/6 域（+ Account + Authorization），z-ctc-admin 可用。

### Phase C：OSC + SURL（1-2 周）

**目标**：4A 6/6 域全覆盖

| 任务                                                                                     | 工作量        |
|----------------------------------------------------------------------------------------|------------|
| C1. z-ctc-core org 包（org unit / staff / position / manage_tree / tag 5-6 张表 + Service） | 2 天        |
| C2. 新建 z-ctc-surl 子模块（1 张表 + Service + Controller）                                     | 1 天        |
| C3. z-ctc-sdk 完善（暴露 6 域 SPI 给 z-camuda / z-task 等业务模块）                                     | 1 天        |
| C4. z-ctc-client facade（统一 6 域 client 调用入口）                                            | 1 天        |
| C5. 集成测试（与 z-camuda 协同：管理树 → 审批链）                                                          | 1 天        |
| C6. 文档完善（4A 中心 API 文档 / 数据字典 / 部署文档）                                                   | 1 天        |
| **小计**                                                                                 | **~1-2 周** |

**交付**：z-ctc 6 域全覆盖，功能自闭环，可独立，**很完善**。

---

## 8. 关键风险

| 风险                                               | 等级 | 缓解                                               |
|--------------------------------------------------|----|--------------------------------------------------|
| z-ctc 现状与 主流 4A 中心产品 10x+ 体量差距                   | 高  | 分阶段实施，每阶段可独立发布                                   |
| RBAC + 数据权限设计过深                                  | 中  | MVP 选 user-role-permission 三表 + 资源类型             |
| spc 20 张表是否全要                                    | 中  | 见 4.3 表级决策                                       |
| 异动/资质等复杂 OSC 流程                                  | 中  | **简化抛弃**（见 4.4）                                  |
| SM2 国密                                           | 低  | 可选，普通场景不强制                                       |
| 历史包路径不统一（`com.zifang.ctc` vs `com.zifang.z.ctc`） | 低  | 增量代码统一用 `com.zifang.z.ctc.*`                     |
| z-ctc 依赖 z-ctc-sso 导致循环                          | 低  | 抽出 z-ctc-common 公共依赖                             |
| z-ctc-audit-starter 异步落库用 MQ                     | 中  | **先线程池**，MQ 后续接                                  |
| 蒸馏参考实现 时不要把私域 groupId 库带过来                       | 高  | 蒸馏时**只取** SQL 设计和 SPI 设计，**不取** 任何私域 groupId 实现类 |

---

## 9. 下一步

需要你决策的 3 件事：

1. **Phase A 范围确认**：1 周做 Authentication + Audit 加固 + z-ctc 独立可跑，是否开干？
2. **OSC 简化程度**：4.4 节建议"org + staff + position + manage_tree + tag 5 张表"——还是更激进（只保留 org + staff 2 张表）？
3. **spc 表裁剪**：4.3 节建议保留 16-17 张表，合并 2 张到 audit——是否更激进（如只保留 8 张核心表）？

收到你的决策后，我会：

- 输出 Phase A 详细 plan（写到 z-ctc/_doc/phase-a-plan.md）
- 开始修 z-ctc-sso + 扩展 z-ctc-audit-starter + 写 z-ctc-spring-boot-starter

---

> **下一步动作建议**：先做 Phase A，1 周可让 z-ctc 独立可跑 + 2 域完整。验证可行性后再开 Phase B / C。
