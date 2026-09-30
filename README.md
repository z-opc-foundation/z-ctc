# z-ctc

> 4A 中心 —— Authentication / Authorization / Account / Audit（认证、授权、账户、审计）

一人公司基座的统一身份与访问控制中心：多租户 + 域隔离的账户体系，签发 JWT，向业务应用提供
登录、鉴权、菜单、审计日志四类能力，并通过 SSO 拦截器把这套身份跨子域共享给前端 SPA 与后端服务。

---

## 📋 基本信息

| 字段 | 值 |
|------|-----|
| **仓库** | `z-ctc`（Comprehensive-Tissue-Centre 缩写，历史上称 4A 中心） |
| **Maven 坐标** | `io.github.yuku123:z-ctc:${revision}` |
| **当前版本** | `1.0.2`（根 POM `<revision>`，CI-friendly versions + flatten-maven-plugin） |
| **父项目** | `io.github.yuku123:z-boot-parent:1.0.21`（`<relativePath/>` 留空，parent 在 repo1 不在磁盘） |
| **Maven Central** | 已发布：`z-ctc:1.0.2` / `z-ctc-web:1.0.2` 均可从 repo1 拉取 |
| **默认端口** | `8888`（dev profile 下 context-path 为 `/ctc`） |
| **运行口径** | Java 8 · Spring Boot 2.7.18 |
| **最近更新** | 2026-09-30 |

---

## 🎯 能力清单

全部能力都能从 `z-ctc-web/src/main/java/com/zifang/ctc/web/api/` 下的 Controller 一一对应：

| 能力 | 入口 | 说明 |
|------|------|------|
| 认证（Authentication） | `AuthnController` | 凭证换令牌、当前身份查询（whoami） |
| 授权（Authorization） | `AuthzController` | 判定用户对目标应用/资源的访问权限 |
| 账户管理（Account） | `AccountController` | 账户增删改查、可访问应用列表 |
| 审计（Audit） | `LoginLogController` | 登录与授权操作日志 |
| 租户 | `TenantController` / `TenantInitAdminController` | 租户域隔离、租户初始化 |
| 域 | `DomainController` | 域名/子域配置，供 SSO cookie 作用域使用 |
| 组织 | `OrgController` | 组织架构 |
| 邀请 | `InvitationController` | 邀请注册链路 |
| 应用菜单 | `MenuController` | 按应用下发菜单 |

跨子域免登由 `z-ctc-sso` 提供：`SsoInterceptor` 校验请求 → `TokenService` 有三种实现
（`LocalTokenService` 本地解析 JWT、`RemoteTokenService` 回源校验、`CompositeTokenService` 组合两者），
`TokenCache` 做令牌缓存，`SsoProperties` 承载 `sso.*` 配置。`JwtUtil` 的签名/JSON/base64url/exp
逻辑已收编到 `z-util` 的 `Jwt`（门面委托），不再自养一份 JWT 实现。

---

## 🏗️ 项目结构

```
z-ctc/
├── pom.xml                # 根聚合 POM：继承 z-boot-parent:1.0.21，<revision> 统一版本
├── z-ctc-common/          # 常量、DTO、跨模块共享的轻量类型
├── z-ctc-core/            # 4A 领域核心：实体、MyBatis-Plus Mapper、服务实现
├── z-ctc-sso/             # SSO：拦截器 + 令牌服务 + cookie 域 + JWT 门面
├── z-ctc-web/             # HTTP 层：Controller、自动装配（ZCompanyCtcWebAutoConfiguration）
├── z-ctc-admin/           # 可启动演示应用（不进 Maven Central）
├── bootstrap/             # 脚手架产物目录，不参与 Maven 发布
├── _frontend/             # 配套前端两层
│   ├── z-ctc-frontend/                    # 业务应用
│   └── z-ctc-frontend-component/          # 组件库
├── deploy/                # 部署资产（三种模式 + k8s）
└── _doc/                  # 文档，见文末「文档目录」
```

`z-ctc-admin` 留在 reactor 里是为了享受统一构建，但其 POM 设了 `maven.deploy.skip=true`，
产物只作 Docker 镜像来源或本地 `java -jar` 演示，**永远不会上 Maven Central**。

---

## 🔧 技术栈

| 层级 | 技术 |
|------|------|
| 语言 / 运行时 | Java 8（全组织口径 1.8） |
| 框架 | Spring Boot 2.7.18（版本由 `z-boot-dependencies` 地板统一供给） |
| 持久层 | MyBatis-Plus 3.5.3.1 + Druid 连接池 |
| 数据库 | MySQL 8.0（生产）/ H2（`dev` profile，免外部依赖启动） |
| 令牌 | JWT（实现收编至 `z-util`，`z-util` 版本走 `z-boot-fleet` 权威表） |
| 接口文档 | Knife4j / springdoc —— 启动后访问 `/doc.html` |
| 前端 | React + Ant Design（`_frontend/`，独立 npm 工程） |
| 构建 | Maven（后端）· npm（前端）· Docker / k8s（部署） |

---

## 🚀 快速开始

### 编译

```bash
mvn clean install -DskipTests
```

第三方版本一律由 `z-boot-parent` → `z-boot-dependencies`（地板）+ `z-boot-fleet`（兄弟仓权威表）
供给，模块 POM 里不应再出现字面版本钉；若构建报找不到版本，先确认本地/镜像能解析到
`io.github.yuku123:z-boot-parent:1.0.21`。

### 本地跑起来（无需 MySQL）

```bash
SPRING_PROFILES_ACTIVE=dev java -jar z-ctc-admin/target/z-ctc-admin-1.0.2-exec.jar
```

`dev` profile 排除 `ZCompanyCtcWebAutoConfiguration`（它强制 MySQL Druid 数据源），改用 H2 内存库，
监听 `http://localhost:8888/ctc`，Knife4j 文档在 `http://localhost:8888/ctc/doc.html`。

### 连真实 MySQL

数据源走 `z.base.db.ctc.*` 模块化配置（由 `ZCompanyCtcWebAutoConfiguration#buildDataSource` 装配），
`z.base.db.ctc.*` 缺省时回退 `z.base.db.default.*`。**所有凭据必须经环境变量注入，禁止写进 yml/jar/镜像层**：

| 环境变量 | 用途 |
|----------|------|
| `CTC_DB_URL` / `CTC_DB_USERNAME` / `CTC_DB_PASSWORD` | ctc 模块数据源 |
| `DB_HOST` / `DB_PORT` / `DB_DATABASE` / `DB_USERNAME` / `DB_PASSWORD` | 其它模块拼 URL 的回退路径 |

MySQL 8 直连需 `allowPublicKeyRetrieval=true`，通过 `druid.connection-properties` 注入到所有连接。

---

## 🔌 API 一览

服务前缀统一为 `/api/ctc`，启动后可用 Knife4j 浏览完整 OpenAPI：

| 路径 | Controller |
|------|------------|
| `/api/ctc/authn` | 认证 |
| `/api/ctc/authz` | 授权 |
| `/api/ctc/ac/accounts` | 账户 |
| `/api/ctc/ac/tenants` | 租户 |
| `/api/ctc/ac/domains` | 域 |
| `/api/ctc/ac/invitations` | 邀请 |
| `/api/ctc/ac/login-log` | 登录审计 |
| `/api/ctc/ac` | 组织 |
| `/api/ctc/app-menu` | 应用菜单 |
| `/api/ctc/admin/tenant-init` | 租户初始化 |

---

## 🔐 SSO 接入

`z-ctc-sso` 通过 `SsoAutoConfiguration` 读取 `sso.*` 前缀。消费方关心的键：

| 配置 | 环境变量 | 说明 |
|------|----------|------|
| `sso.enabled` | `SSO_ENABLED` | 总开关 |
| `sso.callback-url` | `SSO_CALLBACK_URL` | 回调地址，生产必须覆盖 |
| `sso.cookie-domain` | `SSO_COOKIE_DOMAIN` | `sso_token` cookie 落到的根域。**必须是真实根域**（如 `.zopc.local`），留默认当前 host 会导致跨子域不发 cookie → 鉴权失败 → 反复重定向登录 |
| `sso.cookie-secure` | `SSO_COOKIE_SECURE` | HTTPS 部署置 true |
| `sso.login-url` | `SSO_LOGIN_URL` | `SsoInterceptor.sendRedirect` 的跳转目标（前端 SPA 登录页） |
| `sso.exclude-paths` | — | 放行路径（登录、注册、`/doc.html`、`/actuator/**` 等） |
| `sso.intercept-paths` | — | 拦截路径，默认 `/api/**` |

注意：`whoami` 这类需要 `SsoInterceptor` 注入 `ssoUser` 的端点**不能**放进 `exclude-paths`。

---

## 🧪 测试与验证

```bash
mvn test
```

端到端验证脚本与三模式启动脚本在 `deploy/`（`bin/start-mode1.sh` ~ `start-mode3.sh`、
`bin/build-images.sh`、`v4-test/`），k8s 清单在 `deploy/k8s/`，按 `00-namespace` → `05-ingress` 顺序 apply。

---

## 🐳 部署

```bash
cd deploy
make help            # 查看目标
# 单机
docker compose -f docker-compose.yml up -d
# 拆分 / 集群
docker compose -f docker-compose.split.yml up -d
docker compose -f docker-compose.cluster.yml up -d
```

镜像构建：`deploy/Dockerfile.backend`（后端，来自 `z-ctc-admin` 产物）+ `deploy/Dockerfile.frontend`
（前端，nginx 模板见 `deploy/nginx.conf.template`）。

---

## 📄 License

许可证见仓库根 [`LICENSE`](LICENSE)；根 POM 声明 MIT License。

_Maintained by the z-opc-foundation organization._

---

## 文档目录

本项目文档统一收口在 `_doc/` 下:

- [`_doc/001_arch/`](_doc/001_arch/) — 架构文档 (项目总览 / 模块结构 / 接口清单 / DB schema / 前端 / 能力 / roadmap):
  - [`00-overview.md`](_doc/001_arch/00-overview.md)
  - [`01-module-structure.md`](_doc/001_arch/01-module-structure.md)
  - [`01-module-structure-2.md`](_doc/001_arch/01-module-structure-2.md)
  - [`02-api.md`](_doc/001_arch/02-api.md)
  - [`03-db-schema.md`](_doc/001_arch/03-db-schema.md)
  - [`04-4a-migration.md`](_doc/001_arch/04-4a-migration.md)
  - [`05-frontend.md`](_doc/001_arch/05-frontend.md)
  - [`07-roadmap.md`](_doc/001_arch/07-roadmap.md)
  - [`z-ctc-admin.md`](_doc/001_arch/z-ctc-admin.md)

- [`_doc/002_deploy/`](_doc/002_deploy/) — 部署 SQL（多文件，放 `init/`，按下面顺序执行）:
  - [`init/init-database.sql`](_doc/002_deploy/init/init-database.sql) — 建库
  - [`init/`](_doc/002_deploy/init/) — 建表与初始化：
    [`schema.sql`](_doc/002_deploy/init/schema.sql)、
    [`init_tables.sql`](_doc/002_deploy/init/init_tables.sql)、
    [`z-ctc-ac.sql`](_doc/002_deploy/init/z-ctc-ac.sql)、
    [`z-ctc-authz.sql`](_doc/002_deploy/init/z-ctc-authz.sql)、
    [`z-ctc-authz-override.sql`](_doc/002_deploy/init/z-ctc-authz-override.sql)、
    [`code-based-schema.sql`](_doc/002_deploy/init/code-based-schema.sql)、
    [`code-based-migration.sql`](_doc/002_deploy/init/code-based-migration.sql)

- [`_doc/003_script/`](_doc/003_script/) — 运维脚本:
  - [`build.sh`](_doc/003_script/build.sh)
  - [`package.sh`](_doc/003_script/package.sh)
  - [`docker-entrypoint.sh`](_doc/003_script/docker-entrypoint.sh)
  - [`deploy_maven_center.sh`](_doc/003_script/deploy_maven_center.sh)
  - [`install-settings.sh`](_doc/003_script/install-settings.sh)

- `_doc/004_skill/` — AI skill 定义（目前为空目录，暂无 skill）

表结构说明另见 [`_doc/001_arch/03-db-schema.md`](_doc/001_arch/03-db-schema.md)；
`04-4a-migration.md` 记录 4A 模型迁移过程。

各文档详细说明见各子目录。
