# z-ctc 4A 认证中心 - 开发计划

## 一、模块概述

z-ctc 是整个平台的 4A 认证中心（认证/账号/权限/审计），是平台基座的核心模块。

### 核心能力

| 能力                 | 说明                              |
|--------------------|---------------------------------|
| 认证（Authentication） | 用户名密码登录、短信/邮箱验证码登录、OAuth2 第三方登录 |
| 账号（Account）        | 用户管理、角色管理、权限管理                  |
| 权限（Authorization）  | 菜单/按钮/API 级别权限、租户域隔离            |
| 审计（Audit）          | 操作审计日志、登录日志                     |

### 现状总结

- **后端骨架**：基本完整，Controller/Service/Mapper 链路完整
- **前端**：已完整实现，位于 `z-opc-main-starter-frontend/src/pages/ctc/`，含登录/注册/用户/角色/权限/审计/租户/域等全部页面
- **Stub 问题**：多处关键逻辑是空实现（角色权限分配、用户角色分配、Token 注销、审计日志）
- **数据库 Schema**：存在两套命名不一致问题（z_ctc_* vs sys_*）
- **短信/邮件**：验证码直接返回，未接入发送服务

---

## 二、当前实现详情

### 2.1 子模块

```
z-ctc/
├── z-ctc-core/          # 核心：实体/Mapper/Service
├── z-ctc-web/           # REST API（/auth、/account、/permission、/tenant、/domain、/org、/dept、/group）
├── z-ctc-admin/         # Spring Boot 启动类 + Knife4j 配置
├── z-ctc-client/        # 空占位（待实现）
├── z-ctc-sdk/           # 空占位（待实现）
├── z-ctc-sso/           # SSO/JWT 工具类
└── bootstrap/
    └── bootstrap-gennerate/  # MyBatis Plus 代码生成器模板
```

### 2.2 数据库表

```
z_ctc_tenant         租户表
z_ctc_user           用户表
z_ctc_role           角色表
z_ctc_permission     权限表（MENU/BUTTON/API）
z_ctc_user_role      用户-角色关联
z_ctc_role_permission 角色-权限关联
z_ctc_audit_log      审计日志表（实体存在，但无写入逻辑）
z_ctc_domain         域表（按租户隔离）
z_ctc_org            组织表
z_ctc_dept           部门表
z_ctc_group          群组表
z_ctc_verify_code    验证码表
```

### 2.3 已实现的 API

| 路径                                      | 方法             | 说明        | 状态             |
|-----------------------------------------|----------------|-----------|----------------|
| `/auth/login`                           | POST           | 用户名密码登录   | ✅ 完整           |
| `/auth/logout`                          | POST           | 注销        | ⚠️ 空实现         |
| `/auth/refresh`                         | POST           | 刷新 Token  | ✅ 完整           |
| `/auth/register/send-code`              | POST           | 发送注册验证码   | ⚠️ 假实现（直接返答案例） |
| `/auth/register/phone`                  | POST           | 手机号注册     | ✅ 完整           |
| `/auth/register/email`                  | POST           | 邮箱注册      | ✅ 完整           |
| `/auth/register/username`               | POST           | 用户名注册     | ✅ 完整           |
| `/auth/login/phone`                     | POST           | 手机验证码登录   | ⚠️ 假实现（未发短信）   |
| `/auth/reset-password/send-code`        | POST           | 发送重置密码验证码 | ⚠️ 假实现         |
| `/auth/reset-password/phone`            | POST           | 手机重置密码    | ✅ 完整           |
| `/auth/reset-password/email`            | POST           | 邮箱重置密码    | ✅ 完整           |
| `/account/info`                         | GET            | 当前用户信息    | ✅ 完整           |
| `/account/list`                         | GET            | 用户列表      | ✅ 完整           |
| `/account/{id}`                         | GET/PUT/DELETE | 用户 CRUD   | ✅ 完整           |
| `/account/{userId}/roles`               | POST           | 分配角色      | ⚠️ 空实现         |
| `/account/change-password`              | POST           | 修改密码      | ✅ 完整           |
| `/account/{userId}/reset-password`      | POST           | 重置他人密码    | ✅ 完整           |
| `/permission/list`                      | GET            | 权限列表      | ✅ 完整           |
| `/permission/{id}`                      | GET/PUT/DELETE | 权限 CRUD   | ✅ 完整           |
| `/permission/role/list`                 | GET            | 角色列表      | ✅ 完整           |
| `/permission/role/{id}`                 | GET/PUT/DELETE | 角色 CRUD   | ✅ 完整           |
| `/permission/role/{roleId}/permissions` | POST/GET       | 角色权限分配    | ⚠️ 空实现         |
| `/tenant/list`                          | GET            | 租户列表      | ✅ 完整           |
| `/tenant/{id}`                          | GET/PUT/DELETE | 租户 CRUD   | ✅ 完整           |
| `/domain/list`                          | GET            | 域列表       | ✅ 完整           |
| `/domain/{id}`                          | GET/PUT/DELETE | 域 CRUD    | ✅ 完整           |
| `/org/list`                             | GET            | 组织列表      | ✅ 完整           |
| `/dept/list`                            | GET            | 部门列表      | ✅ 完整           |
| `/group/list`                           | GET            | 群组列表      | ✅ 完整           |

---

## 三、待办事项

### 3.1 数据层修复

| 序号 | 事项             | 问题                                                  | 优先级 |
|----|----------------|-----------------------------------------------------|-----|
| D1 | 统一数据库 Schema   | Java 实体用 `z_ctc_*`，部分 Mapper 查 `sys_*`，两套 SQL 文件不一致 | P0  |
| D2 | 租户隔离落地         | 查询需强制带 tenant_id 过滤，不能全表扫描                          | P0  |
| D3 | 域/组织/部门/群组层级关系 | Domain → Org → Dept → Group 层级需在查询时传递 domain_id     | P1  |

### 3.2 认证模块

| 序号 | 事项                     | 问题                                                               | 优先级 |
|----|------------------------|------------------------------------------------------------------|-----|
| A1 | **接入短信发送服务**           | `/auth/register/send-code` 和 `/auth/login/phone` 验证码直接返答案例，未真正发送 | P0  |
| A2 | **接入邮件发送服务**           | 邮箱验证码 `/auth/reset-password/send-code` 同上                        | P0  |
| A3 | **Token 黑名单/注销**       | `logout()` 空实现，Token 注销后仍可用                                      | P0  |
| A4 | **Token 主动失效**         | 改密码/角色变更时需让相关 Token 失效                                           | P1  |
| A5 | **SSO Auth Server 端点** | `RemoteTokenService` 调用 `authServerUrl/verify`，但该端点不存在           | P1  |
| A6 | **登录日志**               | `sys_login_log` 表存在，但无写入逻辑                                       | P1  |
| A7 | **第三方 OAuth2**         | GitHub/微信登录能力预留，需接入                                              | P2  |

### 3.3 账号权限模块

| 序号 | 事项         | 问题                                               | 优先级 |
|----|------------|--------------------------------------------------|-----|
| P1 | **角色权限分配** | `assignPermissions()` 空实现，`role_permission` 表无写入 | P0  |
| P2 | **用户角色分配** | `assignRoles()` 空实现，`user_role` 表无写入             | P0  |
| P3 | **用户权限查询** | `selectPermissionsByUserId()` 忽略 userId，返回所有权限   | P0  |
| P4 | **角色权限查询** | `selectPermissionsByRoleId()` 忽略 roleId，返回所有权限   | P0  |
| P5 | **权限点检**   | `checkPermission(userId, permCode)` 能力，权限拦截时校验   | P1  |

### 3.4 审计模块

| 序号 | 事项           | 问题                                                      | 优先级 |
|----|--------------|---------------------------------------------------------|-----|
| L1 | **操作审计日志写入** | `AuditManagerController` 空壳，`AuditLogService` 无实现，操作不记录 | P1  |
| L2 | **审计日志查询**   | `AuditManagerController` 有 CRUD 接口但无 Service 实现         | P1  |
| L3 | **审计日志切面**   | 建议用 AOP 统一拦截所有 Controller 操作，自动写入 `z_ctc_audit_log`     | P1  |

### 3.5 前端（已完整实现）

前端统一位于 `z-opc-main-starter-frontend/src/pages/ctc/`，使用 React Router v7 + Ant Design 6.3.3。

| 页面   | 路径                | 状态    |
|------|-------------------|-------|
| 登录页  | `/ctc/login`      | ✅ 已实现 |
| 概览页  | `/ctc/overview`   | ✅ 已实现 |
| 仪表盘  | `/ctc/dashboard`  | ✅ 已实现 |
| 用户管理 | `/ctc/user`       | ✅ 已实现 |
| 角色管理 | `/ctc/role`       | ✅ 已实现 |
| 权限管理 | `/ctc/permission` | ✅ 已实现 |
| 审计日志 | `/ctc/audit`      | ✅ 已实现 |
| 租户管理 | `/ctc/tenant`     | ✅ 已实现 |
| 域管理  | `/ctc/domain`     | ✅ 已实现 |
| 组织管理 | `/ctc/org`        | ✅ 已实现 |
| 部门管理 | `/ctc/dept`       | ✅ 已实现 |
| 群组管理 | `/ctc/group`      | ✅ 已实现 |

---

## 四、技术要点

### 4.1 JWT Token 结构

```json
{
  "userId": 1,
  "username": "admin",
  "tenantId": "TENANT001",
  "roles": ["ADMIN", "USER"],
  "permissions": ["/api/ctc/*", "/api/task/*"],
  "iat": 1234567890,
  "exp": 1234654290
}
```

- 算法：HS256，自实现（无 Spring Security 依赖）
- 有效期：24h（可配置）
- 刷新机制：refreshToken 换取新 Token
- 注销：存入 Redis 黑名单（或 MySQL Token 黑名单表）

### 4.2 租户域隔离模型

```
租户（Tenant）
 └── 域（Domain）     ← 按业务线划分
      └── 组织（Org）  ← 按部门/团队划分
           └── 部门（Dept）
                └── 群组（Group）
```

- 所有带 tenant_id 的查询必须自动注入 tenant 条件
- 建议：MyBatis 拦截器统一注入 tenant_id，避免遗漏
- Domain 是对外产品线的隔离单位（对内算一个租户）

### 4.3 权限校验链路

```
请求 → SsoInterceptor（路径匹配）→ JWT 校验 → 用户信息注入上下文
                                                      ↓
                                           业务方法内权限点检
                                           checkPermission(userId, permCode)
```

- 路径白名单：`/auth/**`、`/doc.html`、`/swagger-ui.html`
- 鉴权路径：从 `sso.exclude-paths` 配置读取

### 4.4 审计日志 AOP

```java
@Around("@within(org.springframework.web.bind.annotation.RestController)")
public Object logAudit(ProceedingJoinPoint point) {
    // 1. 解析请求信息（URL、参数、用户）
    // 2. 执行目标方法
    // 3. 写入 z_ctc_audit_log
}
```

### 4.5 前端技术栈

前端位于 `z-opc-main-starter-frontend/src/pages/ctc/`，使用 **React Router v7 + Ant Design 6.3.3 + Vite**（注意：非 Umi，是原生
react-router-dom）。

- 路由前缀：`/ctc/*`
- 状态管理：React Context
- 权限控制：前端路由守卫 + 后端接口权限校验
- 页面：登录、注册、重置密码、概览、用户、角色、权限、审计、租户、域、组织、部门、群组

---

## 五、分阶段开发计划

### Phase 1：数据层 + 认证闭环（1.5 周）

> 前端已完整实现，本阶段聚焦后端 Stub 修复。

| 序号   | 事项                                | 人天               |
|------|-----------------------------------|------------------|
| P0-1 | 统一数据库 Schema，修复 z_ctc_ vs sys_ 混乱 | 0.5              |
| P0-2 | 接入短信发送服务（z-mq 或直接 HTTP 接入短信网关）    | 1                |
| P0-3 | 接入邮件发送服务                          | 0.5              |
| P0-4 | Token 黑名单（MySQL 表 + 注销逻辑）         | 1                |
| P0-5 | 角色权限分配（assignPermissions）实现       | 1                |
| P0-6 | 用户角色分配（assignRoles）实现             | 1                |
| P0-7 | 用户权限查询（按 userId 返回该用户的权限）         | 1                |
| P0-8 | 角色权限查询（按 roleId 返回该角色的权限）         | 1                |
| P0-9 | Token 主动失效（改密码/角色变更时失效）           | 1                |
|      | **Phase 1 小计**                    | **9 人天 ≈ 1.5 周** |

### Phase 2：账号权限 + 审计（1 周）

> 前端页面已实现，后端审计逻辑缺失。

| 序号   | 事项                           | 人天             |
|------|------------------------------|----------------|
| P1-1 | 登录日志写入                       | 0.5            |
| P1-2 | 审计日志 AOP 切面（Controller 全局拦截） | 1.5            |
| P1-3 | 审计日志 Service + 查询接口实现        | 1              |
| P1-4 | 权限点检接口（checkPermission）      | 0.5            |
| P1-5 | 前端审计日志页面联调（筛选/分页/导出）         | 1              |
|      | **Phase 2 小计**               | **5 人天 ≈ 1 周** |

### Phase 3：租户域隔离 + SSO（1 周）

> 前端页面已实现，后端隔离逻辑缺失。

| 序号   | 事项                                               | 人天               |
|------|--------------------------------------------------|------------------|
| P2-1 | 租户隔离拦截器（MyBatis 拦截器统一注入 tenant_id）               | 2                |
| P2-2 | SSO Auth Server 端点（/auth/verify）实现               | 0.5              |
| P2-3 | Domain → Org → Dept → Group 层级查询（带 domain_id 传递） | 1                |
| P2-4 | SSO 登录前后端联调                                      | 1                |
| P2-5 | 域/组织/部门前端页面后端联调                                  | 1                |
|      | **Phase 3 小计**                                   | **5.5 人天 ≈ 1 周** |

### Phase 4：收尾 + 运营增强（0.5 周）

| 序号 | 事项                     | 人天               |
|----|------------------------|------------------|
| E1 | 操作审计日志详情（请求参数/响应/耗时）   | 0.5              |
| E2 | 密码强度校验                 | 0.5              |
| E3 | 账号锁定（连续输错 N 次密码锁定）     | 0.5              |
| E4 | 文档输出（ Knife4j + 各接口说明） | 0.5              |
|    | **Phase 4 小计**         | **2 人天 ≈ 0.5 周** |

---

## 六、汇总

| 阶段      | 内容                 | 人天                |
|---------|--------------------|-------------------|
| Phase 1 | 数据层修复 + 认证闭环（无前端）  | 9                 |
| Phase 2 | 账号权限 + 审计后端 + 前端联调 | 5                 |
| Phase 3 | 租户域隔离 + SSO + 前端联调 | 5.5               |
| Phase 4 | 收尾（安全增强/文档）        | 2                 |
| **总计**  |                    | **21.5 人天 ≈ 4 周** |

> 注：前端已完整实现，不计入工期估算。
