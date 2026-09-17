# z-ctc-admin

> **Standalone Demo App + Docker 镜像源 —— 永远不上 Maven Central**
>
> 详见 [`lead/005_技术架构/005_前端工程与中间件部署架构规范.md`](../../z-opc-foundation-lead/005_技术架构/005_前端工程与中间件部署架构规范.md) §2

## 它是什么

一个**最小可运行单体应用**，把 `z-ctc-web`（包含 web 层 controllers + services + 4A 自动装配）装进来，
附上 Druid + MyBatis-Plus + Knife4j（OpenAPI 文档）+ spring-security-crypto（密码 BCrypt）+ Actuator，
跑起来就是一个完整的「4A 中心 admin UI」（认证 / 授权 / 账号 / 审计）。

业务方**永远不会**在自己的项目里 `import io.github.yuku123:z-ctc-admin`。
它存在的目的是：

1. **本地 `java -jar` / `mvn spring-boot:run` 演示** —— 让开发者不写一行代码就能跑起来
2. **Docker 镜像素材** —— `deploy/Dockerfile.backend` 把 exec jar 打成
   `ghcr.io/yuku123/z-ctc-admin:1.0.1`，作为 006_部署方案 中 L3 中间件镜像
3. **本地部署 / 试用 / PoC** —— 中小团队不想折腾分体 / 集群，直接 `docker run` 一个容器

## 与 z-schedule-admin 的关键差异

| 维度 | z-schedule-admin | z-ctc-admin |
|---|---|---|
| 依赖 | `z-schedule-spring-boot-starter` | `z-ctc-web`（直接依赖 web 层）|
| 前端注入 | V2 已支持（_frontend/z-schedule-frontend/ → jar）| V2 待 V2.x 改造 |
| 健康端点 | `/meta/actuator/health` | `/actuator/health`（默认）|
| 镜像用途 | 调度中心 UI | 4A 管理 UI |

## 为什么不上 Maven Central

| 产物 | 谁来 import | 发 Central |
|---|---|---|
| `z-ctc-common` | 同仓模块 | ✅ |
| `z-ctc-core` | 同仓模块 | ✅ |
| `z-ctc-sso` | 同仓模块 | ✅ |
| `z-ctc-web` | **业务方 import** | ✅ |
| `z-ctc-admin` | **没人 import**，是 `java -jar` 入口 | ❌ |

中央发布只会污染搜索（`<name>` 含「Standalone」字样）+ 占用 namespace + 没任何引用方。

## 本地启动

### 方式 A：`mvn spring-boot:run`（最快）

```bash
cd z-ctc/z-ctc-admin
mvn spring-boot:run
# 默认端口 8080，访问 http://localhost:8080
```

### 方式 B：编出 exec jar 后启动

```bash
cd z-ctc
mvn -pl z-ctc-admin -am package -DskipTests

java -jar z-ctc-admin/target/z-ctc-admin-1.0.1-exec.jar
```

### 方式 C：docker（与 deploy/ 配合）

```bash
make -C z-ctc/deploy dev
# 或
cd z-ctc/deploy && docker compose up
```

## 凭证管理

- **不在 `application.yml` 写明文密码**（admin 内置 BCrypt 加密）
- 数据库密码通过环境变量 `SPRING_DATASOURCE_PASSWORD` 注入
- 本地开发用 `src/main/resources/application-local.yml`（**gitignored**，仓库里只有 `.example`）

## 模块管理特性

本 module **保留在 reactor**（顶层 `pom.xml` `<modules>` 已启用），所以：

| 命令 | 是否参与 |
|---|---|
| `mvn clean package` | ✅ 编 + 出 exec jar |
| `mvn install` | ✅ 装到 `~/.m2/repository`（docker 构建要用） |
| `mvn -pl z-ctc-admin -am package` | ✅ 只编 admin + 其依赖 |
| `mvn deploy -Pcentral` | ❌ **自动跳过**（`maven.deploy.skip=true`） |
| `mvn verify -Pcentral-dryrun` | ✅ 编 + 校验元信息，但不 deploy |

deploy 脚本（`deploy_maven_center.sh`）显式 `-pl '!z-ctc-admin'` 做双保险。

## 与前端工程的关系

未来 V2 改造后，本目录的 `src/main/resources/static/` 将**由 `_frontend/z-ctc-frontend/` 的 build 产物自动注入**：
- `_frontend/z-ctc-frontend/dist/` → `z-ctc-admin/src/main/resources/static/`
- 触发时机：`mvn package` 的 `process-resources` 阶段（通过 `frontend-maven-plugin`）
- 当前 `static/` 目录里**没有**手放 dist（依赖由 admin 启动后通过 OpenAPI 文档生成前端页面，V2 后会改为 React SPA）

## 不写 4 个发版元信息

`url` / `licenses` / `developers` / `scm` —— 这 4 个是中央要求。
admin 不 deploy → **完全不需要写**，继承 parent 即可。

## 许可

MIT