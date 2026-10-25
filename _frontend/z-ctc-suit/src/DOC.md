# z-ctc-suit

4A 统一身份控制台（z-ctc）的**独立壳**。本页是本壳自身的能力说明 —— 它不是某个业务域的组件包，
而是「把 z-ctc-component 提供的账号 / 组织 / 认证 / 授权 / 审计能力，装进一个自带登录与布局的 SPA」。

- **宿主**：`z-ctc-admin`（Spring Boot，`frontend-maven-plugin` 注入本壳构建产物）
- **组件来源**：`@yuku123/z-ctc-component`（跨仓唯一前端面；账号/组织/租户等页面代码住在它的 `src/components/**`）
- **后端**：z-ctc-admin 的 `/ctc/ac/**` 与 `/ctc/surl/**`（`z-ctc-component/src/components/**/api.js` 内各模块自带 api.js）

---

## 一、能力概览

| 页面 | 路由（相对本壳 `BASE`） | 能力 |
|------|----------------------|------|
| 首页 | `at('/home')` | 工作台首页，概览入口 |
| 账号管理 | `at('/accounts')` | 账号列表、新增 / 停用、分配角色、改密 / 重置口令（`AccountManagement`） |
| 组织管理 | `at('/orgs')` | 租户 / 域 / 组织-部门-组 三级树（`OrgPage`） |
| 租户管理 | `at('/tenants')` | 左树右表：租户维护 + 域维护（`TenantManagement`） |
| 应用管理 | `at('/applications')` | 应用 + API 资产 / 菜单 / 按钮 / 数据（`ApplicationManagement`） |
| 角色管理 | `at('/roles')` | 角色 CRUD（`RoleManagement`） |
| 权限管理 | `at('/permissions')` | 资源 × 角色的授权配置（`PermissionManagement`） |
| 资源权限点 | `at('/resources')` | 按应用分页查全域权限点（`ResourcePage`） |
| 用户权限 | `at('/user-permissions')` | 某用户的角色 + 权限资源树（`UserPermissionPage`） |
| 字典管理 | `at('/dicts')` | 字典类型与字典项维护（`DictPage`） |
| 短链管理 | `at('/surls')` | 生成短码、查元信息、失效（`SurlManagement`） |
| 审计日志 | `at('/audit')` | 按类型 Tabs 隔离的审计中心，登录日志（`AuditPage`） |
| 认证配置 | `at('/authn')` | 密码策略 / 外部身份源 —— **后端端点未就绪，页面给明确空态** |
| 组件文档 | `at('/docs')` | 本文件渲染页 |

⚠ **前缀是环境相关的**：`at()` 取自 `src/routes-base.js` 的 `BASE`，由 vite `define` 注入
（`__CTC_BASE__`）—— 生产构建是**空串**（context-path `/`），开发模式是 `/z-ctc`。
所以「路由」这一列写的是 `at(...)` 表达式而不是字面路径：**不要**把 `/z-ctc/...` 硬编码进别处。

## 二、导出面

本壳是**应用层**（SPA），没有对外发布的 npm 包；它的导出面就是本目录的模块：

```js
// 菜单 + 路由清单（App.jsx 消费）
import { menuItems, routes } from './pages-manifest'
// 路由前缀
import { at } from './routes-base'
```

组件面的导出在 `@yuku123/z-ctc-component`（见其 `src/index.tsx` / `src/pages.ts`）。

## 三、API 端点

以下路径来自 `z-ctc-component/src/components/**/api.js` 的实际调用（不是推测）：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST/PUT/DELETE | `/ctc/ac/accounts` | 账号增删改查、分配角色、改密/重置口令、启停 |
| GET/POST/PUT/DELETE | `/ctc/ac/orgs` | 组织（租户/域/组织三级）增删改查 |
| GET/POST/PUT/DELETE | `/ctc/ac/depts` | 部门增删改查 |
| GET/POST/PUT/DELETE | `/ctc/ac/groups` | 用户组增删改查 |
| GET/POST/PUT/DELETE | `/ctc/ac/tenants` | 租户增删改查 |
| GET/POST/PUT/DELETE | `/ctc/ac/domains` | 域增删改查 |
| GET | `/ctc/ac/login-log/list`、`/ctc/ac/login-log/page` | 登录日志 |
| GET | `/ctc/ac/login-log/recent` | 最近登录 |
| GET | `/ctc/surl` | 短链（见 `z-ctc-component` 的 SurlManagement） |

⚠ 本壳的**页面**并不直接调这些端点 —— 它们由 `@yuku123/z-ctc-component` 内部各模块的 api.js 发起。
上表列出的是「本壳通过组件间接依赖的后端面」，用来回答「这个壳背后有什么」。

## 四、页面说明

### 4.1 首页（`at('/home')`）

本壳的落地页。`App.jsx` 的根路由重定向到 `menuItems[0].key`，即首页。

### 4.2 管理页（`accounts` / `orgs` / `tenants` / `applications` / `roles` / `permissions` / `resources` / `user-permissions` / `dicts` / `surls` / `audit`）

这些路由在本壳里都只是 `z-ctc-component` 组件的**薄包装**（`src/pages/*.jsx` 各两行）：
页面实现、分页四件套、空态 / 错误态都在组件层，本壳不复制第二套。

### 4.3 认证配置（`at('/authn')`）

后端尚未提供认证源配置端点，页面给出明确空态（`EmptyState`），不做假表单。

## 五、配置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `__CTC_BASE__`（vite define） | 生产 `''` / 开发 `'/z-ctc'` | 路由前缀，必须与 z-ctc-admin 的 context-path 一致 |
| vite `base` | 生产 `'/'` / 开发 `'/ctc/'` | 静态资源基路径；与 context-path 不一致就是线上白屏 |
| `LOCAL_SIBLINGS=1` | 关 | 开发时把 `@yuku123/z-ctc-component` 指向平级仓源码 |

dev 的实际地址是 `http://localhost:5174/ctc/z-ctc/home`：`/ctc` 来自 vite `base`，
`/z-ctc` 来自路由前缀 —— 两者都由 `BrowserRouter` 的 `basename` 与 `at()` 分别处理。

## 六、已知限制

- **认证配置只有空态**：密码策略 / LDAP / OAuth2 / SAML 的后端端点尚不存在，菜单项保留但页面如实说明。
- **生产路由前缀是空串** ⇒ 独立部署时 `/home` 与 `/docs` 就是顶层路径；若与其它应用同域部署会撞车。
- `z-ctc-component/src/pages-manifest.tsx` 仍是骨架（只列 `/z-ctc/hello` 与文档页）：
  主壳「系统工作台」的 ctc 域页面按宿主前缀挂，独立壳的路由以**本壳** `src/pages-manifest.jsx` 为准。

## 七、变更记录

| 版本 | 变更 |
|---|---|
| 2026-10-23 | 落地 lead 008 §7：新增本文件 + `src/pages/ComponentDoc.jsx` 薄包装 + 菜单末项「组件文档」 + 对应路由 |
| 2026-10-25 | 菜单改接组件层真实页面（组织 / 租户 / 应用 / 角色 / 权限 / 资源 / 用户权限 / 字典 / 短链 / 审计），删除账号与审计的 mock 页；认证配置改明确空态；页面按路由懒加载；`BrowserRouter` 补 `basename`（此前 dev 的 `/ctc` 前缀与路由表对不上，所有菜单点开都是 404） |
