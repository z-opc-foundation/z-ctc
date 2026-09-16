# 005_前端配套

## 5.1 页面结构

```
src/pages/ctc/
├── login/           ← 登录/注册页
│   └── AuthPage.tsx
├── overview/        ← 总览
├── account/         ← 账号管理
├── permission/      ← 权限管理（菜单树）
├── role/           ← 角色管理
├── tenant/         ← 租户管理
├── domain/         ← 域管理
├── org/            ← 组织管理
├── dept/           ← 部门管理
├── group/          ← 组管理
├── user/           ← 用户管理（基础用户信息）
└── audit/          ← 审计日志
    └── list/
        └── index.tsx   ← 审计日志列表页
```

## 5.2 API 统一入口

所有 CTC 接口通过 `services/request.ts`（baseURL: `/api`）访问，网关自动路由。

```
登录: POST /api/auth/login
注册: POST /api/auth/register
审计: GET  /api/audit/log
账号: POST /api/account/page
...
```

## 5.3 审计日志前端

**页面**：`src/pages/ctc/audit/list/index.tsx`

使用 ProTable，`request` 接收 `{ current, pageSize }` 参数，自动调用 `getAuditList(params)`。

```typescript
// services/api.ts
export const getAuditList = (params?: any) =>
  request.get('/audit/log', { params })

// 页面
<ProTable
  request={(params) => getAuditList(params)}
  columns={[...]}
/>
```

返回格式：`{ data: AuditLogDTO[], total: number }`

## 5.4 request.ts 拦截器

```typescript
// 已配置：
// - 请求头自动附加 JWT Token
// - 401 响应跳转登录页
// - 统一错误提示
```

## 5.5 API 文档

启动应用后访问：`http://localhost:8080/doc.html`
