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
| 账号管理 | `at('/accounts')` | 账号列表（`ListTable`）、新增/停用、分配角色、重置口令 |
| 认证配置 | `at('/authn')` | 认证方式配置（密码策略、第三方登录等） |
| 授权策略 | `at('/authz')` | 角色 / 权限 / 角色-权限映射的配置与查看 |
| 审计日志 | `at('/audit')` | 登录日志与操作审计查询 |
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

### 4.2 账号管理（`at('/accounts')`）

账号列表用基座 `ListTable`（分页四件套 + 客户端过滤 + 空态），行操作提供分配角色、
重置口令、启停。底层调用 `/ctc/ac/accounts`。

### 4.3 认证配置（`at('/authn')`） / 4.4 授权策略（`at('/authz')`）

认证方式与授权策略的配置页，能力同样来自 `z-ctc-component`。

### 4.5 审计日志（`at('/audit')`）

登录日志与操作审计的查询页，数据来自 `/ctc/ac/login-log/**`。

## 五、配置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `__CTC_BASE__`（vite define） | 生产 `''` / 开发 `'/z-ctc'` | 路由前缀，必须与 z-ctc-admin 的 context-path 一致 |
| vite `base` | 生产 `'/'` / 开发 `'/ctc/'` | 静态资源基路径；与 context-path 不一致就是线上白屏 |
| `LOCAL_SIBLINGS=1` | 关 | 开发时把 `@yuku123/z-ctc-component` 指向平级仓源码 |

## 六、已知限制

- **本壳的页面代码有一半在 component 仓**：`z-ctc-component/src/pages-manifest.tsx` 目前只是
  **骨架**（只列了一条 `/z-ctc/hello` 路由），真正的页面文件在**本壳**的 `src/pages/**`
  ⇒ component 的 manifest 与本壳的 manifest 是两套，别混用。
- **生产路由前缀是空串** ⇒ 独立部署时 `/home` 与 `/docs` 就是顶层路径；若与其它应用同域部署会撞车。
- 本壳**不含** z-ctc-component 里的全部能力（例如短链 `/ctc/surl` 由组件提供，但本壳没有对应菜单项）。

## 七、变更记录

| 版本 | 变更 |
|---|---|
| 2026-10-23 | 落地 lead 008 §7：新增本文件 + `src/pages/ComponentDoc.jsx` 薄包装 + 菜单末项「组件文档」 + 对应路由 |
