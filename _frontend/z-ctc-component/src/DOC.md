# z-ctc-component

z-ctc 4A 中心的**可复用前端组件层**（library mode）：账号 / 组织 / 部门 / 用户组 / 租户 / 域 / 角色 / 资源 /
权限 / 审计 / 字典 / 短链这一整套管理组件。主壳「系统工作台」按域接进来后，ctc 域的页面都从这里出。

- **npm 包名**：`@yuku123/z-ctc-component`
- **宿主接入**：`import { ComponentDocPage } from '@yuku123/z-ctc-component'`（主壳取文档页的统一名，lead 008 §7）
  或直接消费各管理组件（`OrgPage` / `AccountManagement` / `RoleManagement` / …）
- **后端**：z-ctc 后端，前缀 `/api`（各 `api.js` 的 `configureApiBaseURL` 默认 `/api`）
- **技术栈**：React 19 + antd 6 + `@yuku123/z-frontend-common`（基座 `createRequest` 统一解包 `Result<T>`）

> ⚠ 本仓与 `z-ctc-suit` 是**两个不同的包**：suit 是 4A 中心的独立运行壳（`private: true`，不发布 npm），
> 页面代码（AccountsPage / AuthnPage / AuthzPage / AuditPage）目前仍住在 suit 的 `src/pages/**` 下。
> 本包承载的是**组件层**（`src/components/**`）—— 两边视角不同、API 大体重叠 —— 本文件描述的是**本包**的能力面。

---

## 一、能力概览

| 能力 | 组件 | 源文件 | 说明 |
|------|------|--------|------|
| 4A 总览 | `CtcOverview` | `components/CtcOverview/index.jsx` | 4A 中心概览入口 |
| 组织 / 部门 / 组 | `OrgPage`、`OrgManagement` | `components/OrgPage/`、`components/OrgManagement/` | 三级树 CRUD；`OrgPage` 自带租户 / 域拉取，`OrgManagement` 由调用方传 `tenants` / `domains` |
| 租户 / 域 | `TenantManagement` | `components/TenantManagement/` | 左树右表：租户维护 + 租户下的域维护 |
| 账号 | `AccountManagement` | `components/AccountManagement/` | 账号列表、新增 / 停用、分配角色、改密 / 重置口令、删除 |
| 角色 | `RoleManagement` | `components/RoleManagement/` | 角色 CRUD（4A AUTHZ 域） |
| 权限 | `PermissionManagement` | `components/PermissionManagement/` | 资源 × 角色的授权配置 |
| 资源（权限点） | `ResourcePage` | `components/ResourcePage/index.jsx` | 全域权限点统一视图，按应用分页查询 |
| 用户权限 | `UserPermissionPage` | `components/UserPermissionPage/index.jsx` | 查某用户的角色 + 权限资源树 |
| 应用 | `ApplicationManagement` | `components/ApplicationManagement/` | split-panel：左应用列表，右「基本信息 + Tabs（API 资产 / 菜单 / 按钮 / 数据）」 |
| 审计 | `AuditPage` | `components/AuditPage/index.jsx` | 审计中心，按类型 Tabs 隔离日志（登录日志等） |
| 字典 | `DictPage` | `components/DictPage/index.jsx` | 字典类型 + 字典项维护 |
| 短链 | `SurlManagement` | `components/SurlManagement/` | 生成短码、查元信息、失效 |
| 通用件 | `UserTable`、`StatusTag` | `components/UserTable.jsx`、`components/StatusTag.jsx` | 可复用的用户表格 / 状态标签 |

### 路由

本包**不自带**路由表；页面由调用方组合（本仓 `src/pages-manifest.tsx` 只提供一条骨架路由 `/z-ctc/hello`）。
主壳工作台按宿主前缀 `/ctc/*` 挂，文档页的实际路径见第五节。

## 二、导出面

```ts
// 主入口
import { ComponentDocPage } from '@yuku123/z-ctc-component';  // 主壳按统一名取文档页（lead 008 §7）
import HelloCtc from '@yuku123/z-ctc-component';               // 默认导出桶（宿主可能依赖其形状）

// 管理组件
import { OrgPage, TenantManagement, AccountManagement, ApplicationManagement,
         RoleManagement, PermissionManagement, SurlManagement,
         DictPage, AuditPage, CtcOverview, OrgManagement, UserTable, StatusTag } from '@yuku123/z-ctc-component';

// API client（顶栏租户 / 域下拉要用）
import { ctcAcTenantApi, ctcAcDomainApi } from '@yuku123/z-ctc-component';

// /pages 子路径
import { routes, appMeta } from '@yuku123/z-ctc-component/pages';
```

- `ComponentDocPage` 是**具名再导出**（`export { default as ComponentDocPage } from './pages/ComponentDoc'`），
  与默认导出桶 `export default { HelloCtc }` **并存** —— 默认桶的形状不动（宿主可能依赖它）。
- `src/index.d.ts` 是手写声明文件（TS 走它、运行时走 `.js`），新增导出必须**同步补进去**，
  否则 TS 侧会报 TS2305「no exported member」而运行期正常。

## 三、API 端点

前缀 `/api`（各 `api.js` 的 `configureApiBaseURL` 可改），下表路径是 `src/components/**/api.js`
与 `src/components/_api/**` 里**实际调用**的路径。

### 3.1 账号（AC 域，`_api/ctcAcAccount.js`）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/ctc/ac/accounts` | 建账号，返回新 ID |
| GET | `/ctc/ac/accounts` | 账号列表（按租户 / 域过滤）或按 `id` 查单条 |
| PUT | `/ctc/ac/accounts` | 改账号（`id` 走请求参数） |
| DELETE | `/ctc/ac/accounts` | 删账号 |
| POST | `/ctc/ac/accounts/status` | 启用 / 停用 |
| POST | `/ctc/ac/accounts/password/change` | 改口令 |
| POST | `/ctc/ac/accounts/password/reset` | 重置口令 |
| POST | `/ctc/ac/accounts/assign-role` | 给账号分配角色 |

### 3.2 租户 / 域（AC 域，`TenantManagement/api.js`）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/ctc/ac/tenants` | 建租户 |
| GET | `/ctc/ac/tenants` | 按 `tenantCode` 查租户 |
| GET | `/ctc/ac/tenants/list` | 租户全量列表 |
| POST | `/ctc/ac/tenants/status` | 启用 / 停用租户 |
| GET | `/ctc/ac/domains/list` | 某租户下的域列表 |
| GET | `/ctc/ac/domains/list-all` | 域全量列表 |
| GET | `/ctc/ac/domains` | 按 `id` 查域 |
| POST | `/ctc/ac/domains` | 建域 |
| PUT | `/ctc/ac/domains` | 改域 |
| DELETE | `/ctc/ac/domains` | 删域 |

### 3.3 组织 / 部门 / 组（AC 域，`OrgManagement/api.js`）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST / GET / PUT / DELETE | `/ctc/ac/orgs` | 组织增删改查；列表走 `/ctc/ac/orgs/list` |
| POST / GET / PUT / DELETE | `/ctc/ac/depts` | 部门增删改查；列表走 `/ctc/ac/depts/list` |
| POST / GET / PUT / DELETE | `/ctc/ac/groups` | 用户组增删改查；列表走 `/ctc/ac/groups/list` |

组织 / 部门 / 组三级都以 `tenantCode` + `domainCode` 为前缀过滤，组再带 `deptCode`；
主键类参数一律走**请求参数**（`?xxx=`），不用路径变量。

### 3.4 授权（AUTHZ 域，`_api/ctcAuthorization.js`）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ctc/authz/resources/page-by-app`、`/list`、`/page`、`/distinct-apps` | 资源（权限点）分页 / 列表 / 按应用 |
| GET / POST / PUT / DELETE | `/ctc/authz/resources` | 资源增删改查 |
| GET | `/ctc/authz/resources/roles` | 某资源上的角色 |
| POST / DELETE | `/ctc/authz/roles/grant-resource`、`/ctc/authz/roles/revoke-resource` | 角色 ↔ 资源 绑定 / 解绑 |
| GET | `/ctc/authz/roles/list`、`/page` | 角色清单 / 分页 |
| GET / POST / PUT / DELETE | `/ctc/authz/roles` | 角色增删改查 |
| GET | `/ctc/authz/users/roles`、`/ctc/authz/users/permissions` | 某用户的角色 / 权限 |
| POST / DELETE | `/ctc/authz/users/grant-role`、`/ctc/authz/users/revoke-role` | 用户 ↔ 角色 授予 / 撤销 |
| GET | `/ctc/authz/roles/resources` | 某角色拥有的资源 |
| POST | `/ctc/authz/check` | 鉴权判定 |

### 3.5 应用（`_api/metaApp.js`）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/meta-app/list` | 应用清单 |
| GET | `/meta-app/get` | 应用详情 |
| POST | `/meta-app` | 建应用 |
| PUT | `/meta-app` | 改应用 |
| DELETE | `/meta-app` | 删应用 |

### 3.6 字典（`_api/dict.js`）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/dict/type/list`、`/dict/type/get` | 字典类型列表 / 详情 |
| POST / PUT / DELETE | `/dict/type` | 字典类型增删改 |
| GET | `/dict/item/list`、`/dict/items/get` | 字典项列表（按 `dictId` 或 `dictCode`） |
| POST / PUT / DELETE | `/dict/item` | 字典项增删改 |

### 3.7 审计 / 登录日志（`_api/loginLog.js`、`_api/ctcAcAccount.js`）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/ctc/ac/login-log/list` | 登录日志列表 |
| GET | `/ctc/ac/login-log/page` | 登录日志分页 |
| GET | `/ctc/ac/login-log/recent` | 最近登录 |
| POST | `/ctc/ac/login-log/list` | 登录日志条件查询（账号页在用） |

### 3.8 短链（`SurlManagement/api.js`）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/ctc/surl/shorten` | 生成短码 |
| GET | `/ctc/surl/info` | 查短码元信息 |
| DELETE | `/ctc/surl` | 失效短码 |
| GET | `/s/{mapKey}` | 短链跳转（前端拼 `origin + /s/{mapKey}`） |

## 四、组件说明

### 4.1 组织 / 部门 / 组（OrgPage、OrgManagement）

左树右表布局，三级联动：租户 → 域 → 组织 → 部门 → 用户组。
`OrgPage` 是**标准页面**：自己拉租户 / 域并维护选中态；`OrgManagement` 是**业务组件**：
租户 / 域由调用方通过 props 传入（本组件不耦合 z-meta），适合嵌进别的页面。

### 4.2 租户 / 域（TenantManagement）

左树右表。租户支持新建、按 code 查询、启停用、全量列表；域挂在选中租户下，支持新建 / 改 / 删。
`ctcAcTenantApi` / `ctcAcDomainApi` 同时从主入口导出，供宿主顶栏的租户 / 域下拉直接调用。

### 4.3 账号（AccountManagement）

列表 + 新增 / 停用 / 分配角色 / 改密 / 重置口令 / 删除。

### 4.4 应用（ApplicationManagement）

split-panel：左侧应用紧凑列表（编辑 / 停用 / 删除），右侧整块替换为「应用基本信息 + Tabs」，
每个 Tab 一张表（API 资产 / 菜单 / 按钮 / 数据）。

### 4.5 授权（RoleManagement / PermissionManagement / ResourcePage / UserPermissionPage）

- **角色**：角色 CRUD。
- **权限**：资源 × 角色的授权配置（授予 / 撤销）。
- **资源**：全域权限点统一视图，按应用分页。
- **用户权限**：输入用户 → 看他的角色 + 权限资源树（用户列表来自 AC 域，角色 / 权限来自 AUTHZ 域）。

### 4.6 审计 / 字典 / 短链

审计中心按类型 Tabs 隔离日志（当前实现登录日志）；字典是类型 + 字典项两级；短链是生成 / 查询 / 失效三件套。

### 4.7 组件文档

本文件。内容以 `?raw` 在编译期内联进 bundle（零额外请求）；渲染用基座
`@yuku123/z-frontend-common/ui` 的 `ComponentDoc`（左 240px TOC + 右正文两栏）。

## 五、路由与宿主接入

| 场景 | 路径 | 说明 |
|---|---|---|
| 独立壳（`z-ctc-suit`） | `/z-ctc/docs` | 本包 `src/pages-manifest.tsx` 里 `routes` 的路径；独立壳把 `routes` 数组直接铺成 `<Route>` |
| 主壳「系统工作台」 | **`/ctc/docs`** | 工作台按**宿主前缀**挂各域（`/ctc/*`），文档页由主壳 `SystemShell.tsx` 的路由表指向本包导出的 `ComponentDocPage` |

⚠ 两者路径**不同源**：manifest 的 `/z-ctc/docs` 只在独立壳里生效；工作台里走宿主前缀 `/ctc/docs`
（主壳的路由表注释写明：manifest 的 routes 是给独立壳用的，工作台按宿主前缀挂，manifest 路径在工作台不生效）。

## 六、配置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| API 前缀 | `/api` | 各 `api.js` 的 `configureApiBaseURL(baseURL)` 可覆写 |
| 超时 | 15000ms | `createRequest({baseURL, timeout})` |
| 响应解包 | 基座统一拦截器 | 业务层拿到的已经是内层数据，**不要**再 `.then(r => r.data)` |
| 参数规约 | 请求参数 | 主键 / 编码一律 `?xxx=`，不用路径变量 |

## 七、已知限制

- **认证策略配置（密码策略、第三方登录）还没有迁进本包** —— 那张页面目前仍住在 `z-ctc-suit/src/pages/AuthnPage.jsx`，
  本包不含对应组件与端点。
- **本包不含路由表**：真实页面路由由宿主（独立壳 / 主壳）决定，本包只提供组件与 `routes` 骨架。
- `src/index.d.ts` 是手写声明文件，**新增导出必须同步补**，否则 TS 侧报 TS2305 而运行期正常。
- `OrgManagement` 依赖调用方传入 `tenants` / `domains`，不自己拉租户域（避免耦合 z-meta）。
- 主键类参数走请求参数，URL 变长；后端因此不能靠路径变量路由。

## 八、变更记录

| 版本 | 变更 |
|---|---|
| 1.0.4（本次改动，版本号未抬） | 落地 lead 008 §7：新增本文件 + `src/pages/ComponentDoc.tsx` 薄包装 + `pages-manifest` 里 `/z-ctc/docs` 路由 + 主入口（与 `index.d.ts`）具名再导出 `ComponentDocPage` |
| 1.0.4 | 4A 组件层：账号 / 组织 / 部门 / 组 / 租户 / 域 / 角色 / 资源 / 权限 / 应用 / 审计 / 字典 / 短链 |
