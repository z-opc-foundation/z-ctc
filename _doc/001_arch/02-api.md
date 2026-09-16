# 003_接口清单

## 3.1 LoginController

| 方法   | 路径                         | 说明      |
|------|----------------------------|---------|
| GET  | `/api/auth/status`         | 查询登录状态  |
| POST | `/api/auth/login`          | 用户名密码登录 |
| POST | `/api/auth/register`       | 用户注册    |
| POST | `/api/auth/send-code`      | 发送验证码   |
| POST | `/api/auth/reset-password` | 重置密码    |

## 3.2 AccountManagerController

| 方法   | 路径                         | 说明     |
|------|----------------------------|--------|
| POST | `/api/account/page`        | 分页查询账号 |
| GET  | `/api/account/{id}`        | 账号详情   |
| POST | `/api/account`             | 新增账号   |
| POST | `/api/account/{id}/update` | 更新账号   |
| POST | `/api/account/{id}/delete` | 删除账号   |

## 3.3 PermissionManagerController

| 方法   | 路径                            | 说明     |
|------|-------------------------------|--------|
| POST | `/api/permission/page`        | 分页查询权限 |
| GET  | `/api/permission/{id}`        | 权限详情   |
| POST | `/api/permission`             | 新增权限   |
| POST | `/api/permission/{id}/update` | 更新权限   |
| POST | `/api/permission/{id}/delete` | 删除权限   |

## 3.4 OrgManagerController / DeptManagerController / GroupManagerController

| 方法   | 路径                     | 说明            |
|------|------------------------|---------------|
| POST | `/api/org/page`        | 分页查询（组织/部门/组） |
| GET  | `/api/org/{id}`        | 详情            |
| POST | `/api/org`             | 新增            |
| POST | `/api/org/{id}/update` | 更新            |
| POST | `/api/org/{id}/delete` | 删除            |

## 3.5 AuditManagerController

| 方法   | 路径                  | 说明           |
|------|---------------------|--------------|
| GET  | `/api/audit/log`    | 分页查询审计日志     |
| GET  | `/api/audit/export` | 导出 CSV       |
| POST | `/api/audit/event`  | 接收上报事件（内部接口） |

## 3.6 RoleManagerController

| 方法   | 路径                      | 说明     |
|------|-------------------------|--------|
| POST | `/api/role/page`        | 分页查询角色 |
| GET  | `/api/role/{id}`        | 角色详情   |
| POST | `/api/role`             | 新增角色   |
| POST | `/api/role/{id}/update` | 更新角色   |
| POST | `/api/role/{id}/delete` | 删除角色   |

## 3.7 TenantManagerController

| 方法   | 路径                        | 说明     |
|------|---------------------------|--------|
| POST | `/api/tenant/page`        | 分页查询租户 |
| GET  | `/api/tenant/{id}`        | 租户详情   |
| POST | `/api/tenant`             | 新增租户   |
| POST | `/api/tenant/{id}/update` | 更新租户   |
| POST | `/api/tenant/{id}/delete` | 删除租户   |

## 3.8 DomainManagerController

| 方法   | 路径                        | 说明    |
|------|---------------------------|-------|
| POST | `/api/domain/page`        | 分页查询域 |
| GET  | `/api/domain/{id}`        | 域详情   |
| POST | `/api/domain`             | 新增域   |
| POST | `/api/domain/{id}/update` | 更新域   |
| POST | `/api/domain/{id}/delete` | 删除域   |

**说明**：所有 POST 分页查询的 body 格式统一为 `{ current, pageSize, [filters] }`。
