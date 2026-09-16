# 004_数据库设计

## 4.1 表结构

| 表名                      | 说明      | 关键字段                                                                                                                |
|-------------------------|---------|---------------------------------------------------------------------------------------------------------------------|
| `z_ctc_account`         | 账号表     | id, username, password, email, phone, status, tenant_id                                                             |
| `z_ctc_permission`      | 权限表     | id, **parent_id**（树形）, name, perm_key, **perm_type**（MENU/BUTTON/API）, url, sort, status                            |
| `z_ctc_role`            | 角色表     | id, name, code, status, remark                                                                                      |
| `z_ctc_account_role`    | 账号-角色关联 | account_id, role_id                                                                                                 |
| `z_ctc_role_permission` | 角色-权限关联 | role_id, permission_id                                                                                              |
| `z_ctc_audit_log`       | 审计日志    | id, trace_id, application, operation_type, user_id, user_name, ip, url, method, params, duration, status, error_msg |
| `z_ctc_org`             | 组织表     | id, parent_id（树形）, name, code, sort                                                                                 |
| `z_ctc_dept`            | 部门表     | id, parent_id（树形）, name, code, org_id                                                                               |
| `z_ctc_group`           | 组表      | id, name, dept_id                                                                                                   |
| `z_ctc_tenant`          | 租户表     | id, name, code, status                                                                                              |
| `z_ctc_domain`          | 域表      | id, tenant_id, name, code                                                                                           |

## 4.2 权限树设计（Permission）

`perm_type` 枚举值：

- `MENU`：菜单节点，用于构建菜单树
- `BUTTON`：按钮节点，挂在菜单下，用于按钮级权限控制
- `API`：接口节点，用于 API 级别的权限校验

`parent_id` 实现树形结构，`sort` 字段控制同层排序。

## 4.3 审计日志字段（AuditLog）

| 字段             | 类型           | 说明                               |
|----------------|--------------|----------------------------------|
| id             | BIGINT       | 主键                               |
| trace_id       | VARCHAR(64)  | 链路追踪 ID                          |
| application    | VARCHAR(64)  | 调用方应用名                           |
| operation_type | VARCHAR(32)  | 操作类型（QUERY/CREATE/UPDATE/DELETE） |
| operation_desc | VARCHAR(255) | 操作描述                             |
| user_id        | BIGINT       | 操作用户 ID                          |
| user_name      | VARCHAR(64)  | 操作用户名                            |
| tenant_code    | VARCHAR(64)  | 租户编码                             |
| ip             | VARCHAR(64)  | 客户端 IP                           |
| user_agent     | VARCHAR(255) | 浏览器标识                            |
| request_url    | VARCHAR(512) | 请求路径                             |
| request_method | VARCHAR(16)  | HTTP 方法                          |
| request_params | TEXT         | 请求参数 JSON                        |
| execution_time | INT          | 执行时长（ms）                         |
| status         | TINYINT      | 1=成功，0=失败                        |
| error_msg      | TEXT         | 异常信息                             |
| create_time    | DATETIME     | 创建时间                             |

SQL 脚本：`z-opc/_doc/004_sql/z-ctc.sql`
