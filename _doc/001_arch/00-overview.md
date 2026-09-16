# 一、项目总览

## 1.1 愿景

一个人可以通过这个基座服务自己，方便地产生业务系统，对用户交付。基座具备扩展能力，随业务增长不断扩充。

## 1.2 技术栈

| 层级  | 技术                                              |
|-----|-------------------------------------------------|
| 后端  | Java 8, Spring Boot 2.7.12, MyBatis-Plus 3.3.1  |
| 前端  | React 19, Umi Max 4.6, Ant Design 6.3, Vite 5.1 |
| 数据库 | MySQL 8.0（Druid 连接池）                            |
| 网关  | Netty 高性能 API 网关（z-gw）                          |
| 构建  | Maven（后端），npm（前端）                               |
| 文档  | Knife4j（`/doc.html`）                            |

## 1.3 模块地图

### 平台基座层（所有平台的前提）

| 模块       | 说明                               | 状态    |
|----------|----------------------------------|-------|
| z-ctc    | 4A 认证中心：用户/角色/权限、租户域隔离、JWT Token | 已有 ✅  |
| z-config | 配置中心：统一配置管理、环境隔离                 | 已有 ✅  |
| z-gw     | API 网关：Netty 高性能路由、统一鉴权          | 已有 ✅  |
| z-mist   | 密钥管理：敏感信息加密存储                    | 已有 ⚠️ |
| z-oss    | 对象存储：文件上传下载                      | 已有 ⚠️ |
| z-meta   | 应用资源元信息                          | 已有 ⚠️ |

### 运维/开发平台层

| 模块         | 说明                       | 状态    |
|------------|--------------------------|-------|
| z-ops      | 运维平台（Go）：Docker 管理、宿主机监控 | 规划中   |
| z-task     | 任务中心：任务创建/分配/跟踪          | 已有 ⚠️ |
| z-wf       | 工作流/审批中心：流程定义、审批路由       | 已有 ⚠️ |
| z-schedule | 调度中心：分布式定时任务             | 已有 ⚠️ |

### 中间件平台层

| 模块      | 说明                        | 状态    |
|---------|---------------------------|-------|
| z-mq    | 消息队列：P2P + Pub/Sub，本地内存兜底 | 已有 ⚠️ |
| z-rpc   | RPC 框架：TCP/Netty，HTTP 降级  | 已有 ⚠️ |
| z-ext   | 业务扩展：SPI 插件化              | 已有 ⚠️ |
| z-cache | JVM 内存缓存（待建）              | 规划中   |

### 智能化平台层

| 模块       | 说明                        | 状态  |
|----------|---------------------------|-----|
| Agent 平台 | 可视化 Agent 制造、Skill/MCP 管理 | 规划中 |
| 产品制造器    | 对话式生成系统                   | 规划中 |

**状态说明**：✅ 完整可用 ⚠️ 骨架完整待完善 ❌ 待新建

## 1.4 目录结构

```
z-opc/
├── bootstraps/
│   ├── z-opc-main-starter/         # 主启动器（打包 z-ctc + z-config）
│   ├── z-opc-main-starter-frontend/ # React 前端
│   ├── z-opc-ops-starter/          # 运维启动器
│   └── z-opc-main-test-starter/
├── z-ctc/                                  # 4A 认证中心
│   ├── z-ctc-core/                         # 核心业务（Entity/Mapper/DomainService）
│   ├── z-ctc-web/                          # Web 层（Controller/Req/Resp）
│   ├── z-ctc-admin/                        # 管理后台
│   ├── z-ctc-sdk/                          # SDK
│   ├── z-ctc-sso/                          # SSO 客户端
│   ├── z-ctc-audit-starter/                # 审计客户端（其他应用引入）
│   └── bootstrap/                           # 启动器
├── z-config/                               # 配置中心
├── z-task/                                 # 任务中心
├── z-wf/                                   # 工作流引擎
├── z-schedule/                             # 调度中心
├── z-mist/                                 # 密钥管理
├── z-oss/                                  # 对象存储
├── z-meta/                                 # 元数据
├── z-gw/                                   # API 网关
├── z-mq/                                   # 消息队列
├── z-rpc/                                  # RPC 框架
├── z-ext/                                  # 业务扩展
├── z-boot/                                 # 共享 starters
│   ├── z-boot-starter/                     # Web/Datasource starters
│   └── z-boot-dependencies/                # BOM
├── _doc/                                   # 文档
└── _sql/                                   # 数据库脚本
```

## 1.5 部署架构

### All-in-One（开发/演示）

```
┌────────────────────────────────────┐
│     z-opc-main-starter     │
│   (z-ctc + z-config + z-gw 等)    │
│              Port: 8888             │
└────────────────────────────────────┘
         Port: 3000
┌────────────────────────────────────┐
│   z-opc-main-starter-frontend │
│           (React 前端)              │
└────────────────────────────────────┘
```

### 分布式集群（生产）

```
                    ┌──────────┐
                    │  Nginx   │
                    └────┬─────┘
                         │ HTTP
                  ┌──────▼──────┐
                  │    z-gw     │ (2+ 实例)
                  │  API Gateway│
                  └──────┬──────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
  ┌─────▼─────┐  ┌──────▼──────┐  ┌─────▼─────┐
  │   z-ctc   │  │  z-config   │  │  z-task   │
  │  集群     │  │   集群       │  │   集群     │
  └───────────┘  └─────────────┘  └───────────┘

        ┌────────────┐  ┌────────────┐
        │  MySQL 8  │  │   Redis    │
        │ 主从集群   │  │  集群      │
        └────────────┘  └────────────┘
```

## 1.6 服务端口

| 服务                            | 端口   |
|-------------------------------|------|
| main-starter（后端 all-in-one）   | 8080 |
| main-starter-frontend（前端 dev） | 3000 |
| z-ctc 独立运行                    | 8092 |
| z-wf 独立运行                     | 8091 |
| z-task 独立运行                   | 8090 |
| z-mist 独立运行                   | 8085 |
| z-meta 独立运行                   | 8093 |
| z-config 独立运行                 | 8848 |

## 1.7 数据库

- **引擎**：MySQL 8.0（Druid 连接池）
- **ORM**：MyBatis-Plus 3.3.1
- **数据库名**：`biz_service`
- **远程地址**：`101.37.80.51:3306`
- **SQL 脚本**：`_sql/` 目录下按模块分 `z-ctc.sql`、`z-wf.sql`、`z-task.sql` 等

## 1.8 构建命令

```bash
# 后端全量构建
mvn clean package

# 后端跳过测试构建
mvn clean package -DskipTests

# 前端开发
cd bootstraps/z-opc-main-starter-frontend
npm install && npm run dev

# 前端构建
npm run build
```
