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
| **当前版本** | `1.0.6`（根 POM `<revision>`，CI-friendly versions + flatten-maven-plugin） |
| **父项目** | `io.github.yuku123:z-boot-parent:1.2.0`（`<relativePath/>` 留空，parent 在 repo1 不在磁盘） |
| **Maven Central** | 已发布：`z-ctc:1.0.2` / `z-ctc-web:1.0.2` 均可从 repo1 拉取（1.0.3 / 1.0.4 目前只有本地构建产物，未见 Central 下载记录） |
| **默认端口** | `8888`（dev profile 下 context-path 为 `/ctc`） |
| **运行口径** | Java 17 · Spring Boot 3.3.13（2026-10-30 SB3 火车全量迁移，`javax.*` → `jakarta.*`） |
| **最近更新** | 2026-10-30 |

---

## 🎯 能力清单

全部能力都能从 `z-ctc-web/src/main/java/com/zifang/ctc/web/api/` 下的 **12 个** Controller 一一对应：

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
| OSC 部门 | `OscDeptController` | 部门树 / 部门列表；挂在 `/ctc-osc/dept`，**不在 `/api/ctc` 前缀下** |
| OSC 员工 | `OscStaffController` | 员工简表；挂在 `/ctc-osc/staff`，**不在 `/api/ctc` 前缀下** |

跨子域免登由 `z-ctc-sso` 提供：`SsoInterceptor` 校验请求 → `TokenService` 有 **3** 种实现
（`LocalTokenService` 本地解析 JWT、`RemoteTokenService` 回源校验、`CompositeTokenService` 组合两者），
`TokenCache` 做令牌缓存，`SsoProperties` 承载 `sso.*` 配置。`JwtUtil` 的签名/JSON/base64url/exp
逻辑已收编到 `z-util` 的 `Jwt`（门面委托），不再自养一份 JWT 实现。

---

## 🏗️ 项目结构

```
z-ctc/
├── pom.xml                # 根聚合 POM：继承 z-boot-parent:1.2.0，<revision> 统一版本
├── z-ctc-common/          # 常量、DTO、跨模块共享的轻量类型
├── z-ctc-core/            # 4A 领域核心：实体、MyBatis-Plus Mapper、服务实现
├── z-ctc-sso/             # SSO：拦截器 + 令牌服务 + cookie 域 + JWT 门面
├── z-ctc-web/             # HTTP 层：Controller、自动装配（ZCompanyCtcWebAutoConfiguration）
├── z-ctc-client/          # 授权数据 HTTP SDK（裸能力，零 Spring 依赖）
├── z-ctc-spring-boot-starter/  # 上面那个 SDK 的 Spring Boot 装配层
├── z-ctc-admin/           # 可启动演示应用（不进 Maven Central）
├── bootstrap/             # 脚手架产物目录（自带 pom，但不在根 POM 的 <modules> 里，不参与 Maven 发布）
├── _frontend/             # 配套前端 4 个 npm 工程
│   ├── z-ctc-suit/                # 业务应用
│   ├── z-ctc-component/          # 组件库
│   ├── z-ctc-frontend/           # 前端壳
│   └── z-ctc-frontend-component/ # 前端壳的组件
├── deploy/                # 部署资产（三种模式 + k8s）
└── _doc/                  # 文档，见文末「文档目录」
```

`z-ctc-admin` 留在 reactor 里是为了享受统一构建，但其 POM 设了 `maven.deploy.skip=true`，
产物只作 Docker 镜像来源或本地 `java -jar` 演示，**永远不会上 Maven Central**。

---

## 🔧 技术栈

| 层级 | 技术 |
|------|------|
| 语言 / 运行时 | Java 17（全组织口径 1.8） |
| 框架 | Spring Boot 2.7.18（版本由 `z-boot-parent` 自带地板统一供给） |
| 持久层 | MyBatis-Plus（`z-ctc-admin` 钉 **3.5.3.1**，其余模块走 `z-boot-parent` 地板 **3.5.7**）+ Druid 连接池 |
| 数据库 | MySQL 8.0（生产）/ H2（`dev` profile，免外部依赖启动） |
| 令牌 | JWT（实现收编至 `z-util`，`z-util` 版本走 `z-boot-parent` 的兄弟仓权威表） |
| 接口文档 | Knife4j / springdoc —— 启动后访问 `/doc.html` |
| 前端 | React + Ant Design（`_frontend/`，独立 npm 工程） |
| 构建 | Maven（后端）· npm（前端）· Docker / k8s（部署） |

---

## 🚀 快速开始

### 编译

```bash
mvn clean install -DskipTests
```

第三方版本一律由 `z-boot-parent:1.2.0` 供给（第三方地板与兄弟仓权威表均已并入 parent 自身），
模块 POM 里不应再出现字面版本钉；若构建报找不到版本，先确认本地/镜像能解析到
`io.github.yuku123:z-boot-parent:1.2.0`。

### 本地跑起来（无需 MySQL）

```bash
SPRING_PROFILES_ACTIVE=dev java -jar z-ctc-admin/target/z-ctc-admin-1.0.4-exec.jar
```

`dev` profile 排除 `ZCompanyCtcWebAutoConfiguration`（它强制 MySQL Druid 数据源），改用 H2 内存库，
监听 `http://localhost:8888/ctc`，Knife4j 文档在 `http://localhost:8888/ctc/doc.html`。

### 连真实 MySQL

数据源走 `z.base.db.ctc.*` 模块化配置（由 `ZCompanyCtcWebAutoConfiguration#buildDataSource` 装配），
`z.base.db.ctc.*` 缺省时回退 `z.base.db.default.*`。**所有凭据必须经环境变量注入，禁止写进 yml/jar/镜像层**：

| 环境变量 | 用途 |
|----------|------|
| `DB_HOST` / `DB_PORT` / `DB_DATABASE` | 拼 `z.base.db.ctc.url`（与 `z.base.db.default.*` 同源） |
| `DB_USERNAME` / `DB_PASSWORD` | `z.base.db.ctc.username` / `z.base.db.ctc.password` |

> `CTC_DB_*`（`CTC_DB_URL` / `CTC_DB_USERNAME` / `CTC_DB_PASSWORD`）三个变量**全仓无定义** ——
> `z-ctc-admin/src/main/resources/application.yml` 的注释里记着这段历史：凭据读的是 `CTC_DB_*`，
> 而 compose 的 ctc 服务只注入 `DB_*`，于是密码恒为空串、连生产库必然认证失败。
> 现已统一到 `DB_*` 这一组，切库只改 `.env` 一处。

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
| `/ctc-osc/dept` | OSC 部门（**不在 `/api/ctc` 前缀下**） |
| `/ctc-osc/staff` | OSC 员工（**不在 `/api/ctc` 前缀下**） |

---

## 🔐 SSO 接入

`z-ctc-sso` 通过 `SsoAutoConfiguration` 读取 `sso.*` 前缀。消费方关心的键：

| 配置 | 环境变量 | 说明 |
|------|----------|------|
| `sso.login-url` | `SSO_LOGIN_URL` | `SsoInterceptor.sendRedirect` 的跳转目标（前端 SPA 登录页） |
| `sso.token-cookie-name` | — | 单值 cookie 名，默认 `sso_token` |
| `sso.token-cookie-names` | — | 多值 cookie 名候选列表，与上一项取并集去重 |
| `sso.intercept-paths` | — | 拦截路径，**`SsoProperties` 的默认值是 `/**`**（不是 `/api/**`） |
| `sso.exclude-paths` | — | 放行路径（登录、注册、`/doc.html`、`/actuator/**` 等） |
| `sso.auth-server-url` | — | 令牌校验服务地址 |
| `sso.ctc-base-url` | — | 4A 中心 base URL；配了 = 本进程是消费方（走 `RemoteTokenService`），留空 = 本进程自己就是 4A 中心（走 `LocalTokenService`） |
| `sso.domain-overrides` | — | 域名 → 租户/域 覆盖规则 |
| `sso.jwt-secret` | `SSO_JWT_SECRET` | HS256 签名密钥（`SsoProperties.jwtSecret`，默认空串） |
| `sso.cookie-domain` | `SSO_COOKIE_DOMAIN` | `sso_token` cookie 落到的根域。yml 默认 `.zopc.local`；生产必须换成真实根域（如 `.your-domain.com`），留当前 host 会导致跨子域不发 cookie → 鉴权失败 → 反复重定向登录 |
| `sso.cookie-secure` | `SSO_COOKIE_SECURE` | HTTPS 部署置 true（yml 默认 `false`） |

> `sso.enabled`（`SSO_ENABLED`）与 `sso.callback-url`（`SSO_CALLBACK_URL`）这两个键**当前无消费方** ——
> `SsoProperties` 里没有对应字段，`SsoAutoConfiguration` 仍会无条件注册拦截器。

注意：`whoami` 这类需要 `SsoInterceptor` 注入 `ssoUser` 的端点**不能**放进 `exclude-paths`。

---

## 🧪 测试与验证

```bash
mvn test
```

本仓是 **Java 8 口径**（`java.version=1.8`，class-file major 52），
用 JDK 8 跑测试；本机默认 `java` 是 17，直接 `mvn test` 不是在声明的口径上跑。

`mvn test` 真正执行的是 **Jupiter 用例**：`z-ctc-sso` 的 `JwtCompatCrossCheckTest` 与
`z-ctc-web` 的 `VerifyCodeServiceTest`。`z-ctc-core` 下的 `AcDeptTreeServiceTest` /
`AcStaffSimpleServiceTest` 与 `z-ctc-sso` 下的 `JwtUtilTest` 是 `main()` 自测，
**surefire 不执行它们**（源文件里没有 Jupiter 注解）—— 要跑得 `javac` + `java` 手工执行。

本 README 的定量主张由 `z-ctc-web/src/test/java/com/zifang/ctc/web/ReadmeClaimsTest.java` 守卫：
版本、reactor 模块清单、Controller 名单、API 前缀、默认端口、SSO 配置键、数据源环境变量、
文档目录逐条对着源码重算，README 改了就红。

端到端验证脚本与三模式启动脚本在 `deploy/`（`bin/start-mode1.sh` ~ `start-mode3.sh`、
`bin/build-images.sh`、`v4-test/`），k8s 清单在 `deploy/k8s/`（`00-namespace.yaml` ~ `08-kapi-ingressroute.yaml` 共 9 个），按编号顺序 apply。

### 验证码 Mock 模式的明文码开关

`VerifyCodeService` 在**没有配置任何 `CodeChannelSender` 通道**时会走 Mock 分支
（all-in-one dev 场景：手机码不可能真发，原本靠日志兜底）。

**明文验证码默认不再打进日志**：

| 配置项 | 默认 | 作用 |
| --- | --- | --- |
| `z.ctc.verify-code.mock-log-plain-code` | `false` | `true` 时才在 Mock 提示里带上明文码 |

关掉它**不影响功能**：验证码照常生成、照常写进 `codeStore`、照常可校验，
只是没人能从日志里看到它。需要靠日志取码的 dev 环境显式打开即可。

理由：这条日志的访问面远大于"知道这个手机号验证码"的人——日志通常被集中收集、
保留期长、可按关键字全文检索；而验证码只有 5 分钟有效期，过期后它就是一条
留在日志里、永不失效的敏感数据。Mock 分支同时改为 `warn` 级别，
因为"没有下发通道"在生产里意味着**用户根本收不到验证码**，值得被注意到。

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

本项目文档统一收口在 `_doc/` 下（3 个桶：`001_arch` / `002_deploy` / `003_script`）:

- [`_doc/001_arch/`](_doc/001_arch/) — 架构文档 (项目总览 / 模块结构 / 接口清单 / DB schema / 前端 / 能力 / roadmap):
  - [`00-overview.md`](_doc/001_arch/00-overview.md)
  - [`01-module-structure.md`](_doc/001_arch/01-module-structure.md)
  - [`02-api.md`](_doc/001_arch/02-api.md)
  - [`03-db-schema.md`](_doc/001_arch/03-db-schema.md)
  - [`04-4a-migration.md`](_doc/001_arch/04-4a-migration.md)
  - [`05-frontend.md`](_doc/001_arch/05-frontend.md)
  - [`06-module-conventions.md`](_doc/001_arch/06-module-conventions.md)
  - [`07-roadmap.md`](_doc/001_arch/07-roadmap.md)
  - [`08-admin.md`](_doc/001_arch/08-admin.md)

- [`_doc/002_deploy/`](_doc/002_deploy/) — 部署 SQL（多文件，放 `init/`）:
  - [`init/init-database.sql`](_doc/002_deploy/init/init-database.sql) — 建库
  - [`init/assemble-init.py`](_doc/002_deploy/init/assemble-init.py) — 把散在 z-ctc / z-opc 两个仓的 DDL 拼成一份有序脚本
  - [`init/00-init-all.sql`](_doc/002_deploy/init/00-init-all.sql) — `assemble-init.py` 的产物；装配顺序是硬约束（见脚本头部注释）
  - [`init/`](_doc/002_deploy/init/) — 建表与初始化的源文件：
    [`schema.sql`](_doc/002_deploy/init/schema.sql)、
    [`init_tables.sql`](_doc/002_deploy/init/init_tables.sql)、
    [`z-ctc-ac.sql`](_doc/002_deploy/init/z-ctc-ac.sql)、
    [`z-ctc-authz.sql`](_doc/002_deploy/init/z-ctc-authz.sql)、
    [`z-ctc-authz-override.sql`](_doc/002_deploy/init/z-ctc-authz-override.sql)、
    [`code-based-schema.sql`](_doc/002_deploy/init/code-based-schema.sql)、
    [`code-based-migration.sql`](_doc/002_deploy/init/code-based-migration.sql)、
    [`z_ctc_ac_dept_add_parent_code.sql`](_doc/002_deploy/init/z_ctc_ac_dept_add_parent_code.sql)

- [`_doc/003_script/`](_doc/003_script/) — 运维脚本:
  - [`build.sh`](_doc/003_script/build.sh)
  - [`package.sh`](_doc/003_script/package.sh)
  - [`docker-entrypoint.sh`](_doc/003_script/docker-entrypoint.sh)
  - [`deploy_maven_center.sh`](_doc/003_script/deploy_maven_center.sh)
  - [`install-settings.sh`](_doc/003_script/install-settings.sh)

- 本仓**没有** `_doc/004_skill/`（无 skill 资产，该目录未创建）

表结构说明另见 [`_doc/001_arch/03-db-schema.md`](_doc/001_arch/03-db-schema.md)；
`04-4a-migration.md` 记录 4A 模型迁移过程。

各文档详细说明见各子目录。

<!-- icon: minimax image-01 placeholder -->
