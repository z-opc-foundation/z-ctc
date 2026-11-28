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
| **当前版本** | `1.0.10`（根 POM `<revision>`，CI-friendly versions + flatten-maven-plugin） |
| **父项目** | `io.github.yuku123:z-boot-parent:1.2.6`（`<relativePath/>` 留空，parent 在 repo1 不在磁盘） |
| **Maven Central** | 已发布：`z-ctc:1.0.2` / `z-ctc-web:1.0.2` 均可从 repo1 拉取（1.0.3 / 1.0.4 目前只有本地构建产物，未见 Central 下载记录） |
| **默认端口** | `8888`（dev profile 下 context-path 为 `/ctc`） |
| **运行口径** | Java 17 · Spring Boot 3.3.13（2026-10-30 SB3 火车全量迁移，`javax.*` → `jakarta.*`） |
| **最近更新** | 2026-10-30 |

---

## 🎯 能力清单

全部能力都能从 `z-ctc-web/src/main/java/com/zifang/ctc/web/api/` 下的 **18 个** Controller 一一对应：

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
| OAuth2 接入 | `OAuthController` | 授权码端点（authorize/token），接入方注册走 `OAuthClientProperties` 配置表 |
| OSC 部门 | `OscDeptController` | 部门树 / 部门列表；挂在 `/ctc-osc/dept`，**不在 `/api/ctc` 前缀下** |
| OSC 员工 | `OscStaffController` | 员工简表；挂在 `/ctc-osc/staff`，**不在 `/api/ctc` 前缀下** |
| 字典 | `DictController` | 字典分类 / 字典项 |
| 建站初始化 | `SiteInitRegistrationController` / `SiteInitRunController` | 建站初始化域注册与执行 |
| 机器 / 服务账号会话 | `ServiceSessionController` | 设备控制台一类的机器访问：令牌签发 / 验签 / 吊销都在认证中心（库里只存哈希，明文仅签发那次响应返回） |
| AK 台账 | `AkController` | AK 签发 / 验签 / scope 授权 / 调用日志（认证中心持有 AK 真源，明文 secret 仅签发那次响应返回，库里只存哈希） |

跨子域免登由 `z-ctc-sso` 提供：`SsoInterceptor` 校验请求 → `TokenService` 有 **3** 种实现
（`LocalTokenService` 本地解析 JWT、`RemoteTokenService` 回源校验、`CompositeTokenService` 组合两者），
`TokenCache` 做令牌缓存，`SsoProperties` 承载 `sso.*` 配置。`JwtUtil` 的签名/JSON/base64url/exp
逻辑已收编到 `z-util` 的 `Jwt`（门面委托），不再自养一份 JWT 实现。

---

## 🏗️ 项目结构

```
z-ctc/
├── pom.xml                # 根聚合 POM：继承 z-boot-parent:1.2.6，<revision> 统一版本
├── z-ctc-common/          # 常量、DTO、跨模块共享的轻量类型
├── z-ctc-core/            # 4A 领域核心：实体、MyBatis-Plus Mapper、服务实现
├── z-ctc-sso/             # SSO：拦截器 + 令牌服务 + cookie 域 + JWT 门面
├── z-ctc-web/             # HTTP 层：Controller、自动装配（ZCompanyCtcWebAutoConfiguration）
├── z-ctc-client/          # 授权数据 HTTP SDK（裸能力，零 Spring 依赖）
├── z-ctc-spring-boot-starter/  # 上面那个 SDK 的 Spring Boot 装配层
├── z-ctc-site-init-starter/  # 建站初始化：域侧 SPI + 4A 自登记装配
├── z-ctc-admin/           # 可启动演示应用（不进 Maven Central）
├── bootstrap/             # 脚手架产物目录（自带 pom，但不在根 POM 的 <modules> 里，不参与 Maven 发布）
├── _frontend/             # 配套前端 2 个 npm 工程
│   ├── z-ctc-suit/                # 业务应用
│   └── z-ctc-component/          # 组件库
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

第三方版本一律由 `z-boot-parent:1.2.6` 供给（第三方地板与兄弟仓权威表均已并入 parent 自身），
模块 POM 里不应再出现字面版本钉；若构建报找不到版本，先确认本地/镜像能解析到
`io.github.yuku123:z-boot-parent:1.2.6`。

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
| `/api/ctc/ac/dict` | 字典 |
| `/api/ctc/site-init` | 建站初始化 |
| `/api/ctc/site-init/registrations` | 建站初始化域注册 |
| `/api/ctc/v1/ak` | AK 台账（签发 / 验签 / scope / 调用日志） |
| `/api/ctc/v1/service-session` | 机器 / 服务账号会话（签发 / 验签 / 吊销 / 会话数） |
| `/api/ctc/oauth` | OAuth2 授权服务器（家族桌面客户端 SSO 入口，见下节） |
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

## 🔑 OAuth2 授权服务器（2026-11-01 新增，家族桌面客户端 SSO 入口）

三个端点（授权码 + PKCE S256），**HTTP 形状各按协议规定，不套 `Result` 包装**：

| 端点 | 鉴权 | 形状 |
|------|------|------|
| `GET /api/ctc/oauth/authorize` | 浏览器登录态（sso_token cookie），未登录跳 `sso.login-url` | 302 回 redirect_uri（带 code / error）；redirect 不合法时渲染 400 错误页**绝不回跳** |
| `POST /api/ctc/oauth/token` | 凭 code 自证（在 exclude-paths 里豁免） | RFC 6749 §5：200 `{access_token, token_type, expires_in}` / 400 `{"error":"invalid_grant"}` |
| `GET /api/ctc/oauth/userinfo` | `Authorization: Bearer <access_token>` | OIDC：`{sub, preferred_username}`（家族 `toSsoUser` 认 `sub`） |

安全基线：PKCE 强制 S256（plain/缺失拒）· redirect_uri **精确匹配**白名单（端口位 `*` = loopback 任意端口）·
code 一次性 + 5 分钟 + 四元组绑定（client/redirect/challenge/user）· 错误码对齐 RFC 6749 §5.2。

接入方注册（`z.ctc.oauth.clients` 配置表，clients 为空 = 三端点全拒）：

```yaml
z:
  ctc:
    oauth:
      clients:
        - client-id: z-biz-tool-remote
          redirect-uris: ["http://127.0.0.1:*/callback"]
          scope: "openid profile email"
```

判据单测：`OAuthAuthorizeServiceTest`（17 例，PKCE 金标取 RFC 7636 附录 B 官方向量）。
客户端侧同一协议的实现对拍用例在 z-biz-tool-shared（30 例 node 单测）。

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

---

<!-- z-opc-foundation-sibling-registry:v1 -->
## 🔗 兄弟仓全景（z-opc-foundation 组织）

本仓隶属于 [z-opc-foundation](https://github.com/orgs/z-opc-foundation/repositories) 组织——一人公司基座的全部自研件（现存 61 仓，含私有仓；口径 2026-11-24 GitHub org API 实读）。

| 仓 | 一句话说明 |
|---|---|
| [z-agent](https://github.com/z-opc-foundation/z-agent) | 云端 Agent 平台的库与 starter——ReAct/Plan 实现 + 注册中心 + REST 网关 + 只读控制面 |
| [z-agent-kernel](https://github.com/z-opc-foundation/z-agent-kernel) | Agent 框架基座——消息/工具/记忆/provider/会话 SPI + 轻量默认实现（纯 POJO 无 Spring） |
| [z-asset](https://github.com/z-opc-foundation/z-asset) | 企业资产台账——实物设备全生命周期 + 域名/证书/许可到期提醒 |
| [z-audit](https://github.com/z-opc-foundation/z-audit) | 审计与合规留痕——事件模型 + 哈希链 + 事件出口 SPI + 留存期策略 |
| [z-biz](https://github.com/z-opc-foundation/z-biz) | 商务外壳域——客户/合同/票据/对账四聚合 |
| [z-biz-erp](https://github.com/z-opc-foundation/z-biz-erp) | 进销存 ERP——商品/客商/采购/销售/库存流水/应收应付核销（z-biz 业务线） |
| [z-biz-security](https://github.com/z-opc-foundation/z-biz-security) | 业务数据安全防护入口——场景化策略注册 + 安全线四段流水线对外编排 |
| [z-boot](https://github.com/z-opc-foundation/z-boot) | Spring Boot Starter 聚合仓 + 版本权威（火车制）——唯一消费入口 z-boot-parent |
| [z-bot](https://github.com/z-opc-foundation/z-bot) | 本地 agent 应用——终端 CLI + HTTP/SSE web 控制台 + 多通道 gateway（飞书/钉钉/webhook） |
| [z-cache](https://github.com/z-opc-foundation/z-cache) | RESP2 协议兼容的分布式内存缓存——Java 8 + Netty，Redis 客户端零修改直连 |
| [z-camuda](https://github.com/z-opc-foundation/z-camuda) | 【已退役·归档】基于 Camunda 的工作流封装层（原 z-wf 改名，与现行 z-wf 无共同历史） |
| [z-cognition](https://github.com/z-opc-foundation/z-cognition) | 业务认知域/认知舱——术语/概念/证据链 + memory/kb/ontology 渐进式模块 |
| [z-config](https://github.com/z-opc-foundation/z-config) | 分布式配置中心 + 服务注册中心（类 Nacos）——Netty 长连接推送 + React 控制台 |
| [z-ctc](https://github.com/z-opc-foundation/z-ctc) | 4A 中心——认证/授权/账户/审计，多租户 JWT + SSO 跨子域 **（本仓）** |
| [z-dms](https://github.com/z-opc-foundation/z-dms) | 数据源管控底座——统一登记、统一建池、统一探活、变更统一推送 |
| [z-env](https://github.com/z-opc-foundation/z-env) | 本地中间件与基础设施环境——31 个 docker-compose 场景 + make 一键运维 |
| [z-etl](https://github.com/z-opc-foundation/z-etl) | ETL 引擎——DataX 式线性作业 + Flink 式 DAG 流管道（键控状态/水位/窗口） |
| [z-ext](https://github.com/z-opc-foundation/z-ext) | Java 扩展点框架——@ExtPoint/@ExtImpl 运行期选实现，五档侵入方式 |
| [z-frontend-common](https://github.com/z-opc-foundation/z-frontend-common) | 全仓共享前端基座——AppLayout/LoginPage 等前端规范的唯一承载 |
| [z-frontend-markdown](https://github.com/z-opc-foundation/z-frontend-markdown) | Markdown 通用共享组件包（npm）——MarkdownLiveEditor/NoteMarkdown/MermaidBlock 单一真源 |
| [z-frontend-render](https://github.com/z-opc-foundation/z-frontend-render) | 平台共享前端渲染引擎——后端 schema 是唯一事实源，报表与低代码共用 |
| [z-graph](https://github.com/z-opc-foundation/z-graph) | 独立图数据库——MVCC 版本链存储 + OpenCypher 子集 + Bolt 风格协议 + React 控制台 |
| [z-gw](https://github.com/z-opc-foundation/z-gw) | 自研高性能 API 网关——Netty 手写运行时，Route/Predicate/Filter 模型可嵌 Spring Boot |
| [z-indexer](https://github.com/z-opc-foundation/z-indexer) | 代码索引服务——仓库注册表 + 定时重建索引 + 符号/调用链查询 |
| [z-introduce](https://github.com/z-opc-foundation/z-introduce) | 组织仓库展示站——全仓实测元数据汇总成可搜索静态站点 |
| [z-lc](https://github.com/z-opc-foundation/z-lc) | 低代码平台——DB 驱动通用 CRUD 引擎 + 报表/模型设计框架 |
| [z-llm-agent](https://github.com/z-opc-foundation/z-llm-agent) | CLI Agent 服务化——把本机 CLI agent（CC/Hermes/Qoder 等）暴露成 OpenAI 兼容对话 API |
| [z-llm-gateway](https://github.com/z-opc-foundation/z-llm-gateway) | LLM 网关——OpenAI/Anthropic 双协议面 + 多 vendor 凭据池、限流与 token 记账 |
| [z-llm-msghub](https://github.com/z-opc-foundation/z-llm-msghub) | msgHub 中心消息路由——WebSocket 中心枢纽 + Agent 侧客户端 SDK |
| [z-mask](https://github.com/z-opc-foundation/z-mask) | 脱敏与遮蔽引擎——按敏感命中位置遮蔽，策略即数据 |
| [z-mcp](https://github.com/z-opc-foundation/z-mcp) | MCP 服务端 + 上游聚合网关——Java 8 手写协议层，Spring Boot 自动装配 |
| [z-meta](https://github.com/z-opc-foundation/z-meta) | 元数据管理平台——租户/应用/接口清单登记 + @BizModel 业务模型采集 |
| [z-mill](https://github.com/z-opc-foundation/z-mill) | 资料磨坊——全格式文档转 Markdown/JSON/纯文本/HTML（对标 MinerU） |
| [z-mist](https://github.com/z-opc-foundation/z-mist) | KMS 分布式密钥/秘密管理平台——集中加密存储 + Netty 协议 + starter 嵌入 |
| [z-mq](https://github.com/z-opc-foundation/z-mq) | 自研分布式消息队列——NameServer+Broker+Store，形状对标 RocketMQ、自有 Netty 协议 |
| [z-msg](https://github.com/z-opc-foundation/z-msg) | 多通道消息 + WebSocket 实时下发基础设施——短信/邮件/站内信/Webhook/IM 平台 |
| [z-observer](https://github.com/z-opc-foundation/z-observer) | 可观测性域——指标/日志/链路 + 告警引擎 |
| [z-opc](https://github.com/z-opc-foundation/z-opc) | 一人公司统一后端+统一前端——单 JVM all-in-one 承载协作/Agent/RPA/交易/资产等业务域与自研中间件 |
| [z-opc-foundation-lead](https://github.com/z-opc-foundation/z-opc-foundation-lead) | 组织治理大脑——事实表、强制规范、跨仓工具集、新机器恢复钥匙（本清单权威持有处） |
| [z-opcs](https://github.com/z-opc-foundation/z-opcs) | 任务撮合/服务市场——用户付费发布诉求，持有者接单完成，金额自动瓜分 |
| [z-ops](https://github.com/z-opc-foundation/z-ops) | 个人公司运维平台——部署单元/镜像构建/WebShell/K8s 巡检 + Ops Agent 执行器 |
| [z-oss](https://github.com/z-opc-foundation/z-oss) | 对象存储服务——自持 local 后端 + aliyun/s3 对接层，starter 注入 OssTemplate |
| [z-product](https://github.com/z-opc-foundation/z-product) | 产品设计信息汇总仓（开发域 PM 工件） |
| [z-qa](https://github.com/z-opc-foundation/z-qa) | 步骤式 API 链路测试平台——用例/计划/执行/报告/质量看板（对标 TestRail·MeterSphere） |
| [z-rpa](https://github.com/z-opc-foundation/z-rpa) | RPA 产品皮——应用市场/需求中心（执行归 z-script，触发归 z-schedule） |
| [z-rpc](https://github.com/z-opc-foundation/z-rpc) | Java 8 + Netty RPC 框架——微内核 + SPI 插件化，形状参照 Dubbo/gRPC |
| [z-schedule](https://github.com/z-opc-foundation/z-schedule) | 分布式任务调度中枢（对标 XXL-Job）——选主 + 时间环排期，starter 嵌入宿主 |
| [z-script](https://github.com/z-opc-foundation/z-script) | 多 DSL 脚本平台 + Mock 平台——沙箱执行/版本灰度/可注册 MCP Tool，starter 内嵌 |
| [z-security](https://github.com/z-opc-foundation/z-security) | 数据安全域编排层——场景→策略→识别→脱敏→水印→审计 四段流水线编排 |
| [z-sense](https://github.com/z-opc-foundation/z-sense) | 敏感识别与分类分级引擎——数据安全线底座（规则目录 + 可插拔识别器 + 分类分级） |
| [z-skill](https://github.com/z-opc-foundation/z-skill) | Skill 平台聚合器——归一化各家 Skill 形状，再按各家 API 形状发回 |
| [z-surl](https://github.com/z-opc-foundation/z-surl) | 短网址服务——短码生成/解析/命中统计，starter 一行集成 |
| [z-task](https://github.com/z-opc-foundation/z-task) | 任务协作平台——项目空间/看板/任务卡片（看板范式，对标 Trello·Teambition） |
| [z-team](https://github.com/z-opc-foundation/z-team) | 数字员工协同域——全栈独立仓，自 z-opc 毕业 |
| [z-trade](https://github.com/z-opc-foundation/z-trade) | 交易域独立仓——自 z-opc reactor 迁出 |
| [z-util](https://github.com/z-opc-foundation/z-util) | 底层 Java 工具库——89 个模块 POM，Java 8/17 口径，按需引用按需升级 |
| [z-vector](https://github.com/z-opc-foundation/z-vector) | 自研 Java 向量数据库——嵌入式+可独立部署，ANN+Payload 过滤，兼容 Qdrant REST |
| [z-voice-demo](https://github.com/z-opc-foundation/z-voice-demo) | 语音链路 demo——流式识别→kernel LLM→流式 TTS→可打断 的契约验证实现 |
| [z-watermark](https://github.com/z-opc-foundation/z-watermark) | 水印引擎——显式文字/图片隐写/盲水印，三种技术成对「打+验」 |
| [z-webide](https://github.com/z-opc-foundation/z-webide) | 企业级 WebIDE 后端——Git 仓/共享目录秒变 code-server 实例，全生命周期管理 |
| [z-wf](https://github.com/z-opc-foundation/z-wf) | 自研流程引擎——不依赖 Camunda/Flowable，定义层与运行时全自研 |

**已退役仓**（迁至 [z-trash](https://github.com/z-trash) 组织，archived 只读）：

| 仓 | 说明 |
|---|---|
| [z-agent-proxy](https://github.com/z-trash/z-agent-proxy) | 三方 coding agent 适配代理（Aider/Cursor → kernel Agent SPI），2026-10-26 退役 |
| [z-kb](https://github.com/z-trash/z-kb) | 自研知识库系统（对标 LightRAG/RAGFlow），2026-11 并入 z-cognition 认知舱后退役 |
| [z-ontology](https://github.com/z-trash/z-ontology) | 本体层（TBox/ABox/RDFS 推理），并入 z-cognition 后退役 |

另有 [z-report](https://github.com/z-opc-foundation/z-report)（报表域，2026-10-11 撤销并入 z-lc（远端已删除））。

总清单权威持有处在 [z-opc-foundation-lead](https://github.com/z-opc-foundation/z-opc-foundation-lead) `008_组织规范/005_兄弟仓清单.md`。
