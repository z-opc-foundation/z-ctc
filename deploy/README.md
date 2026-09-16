# deploy/

> z-ctc 仓的**部署资源目录**。按规范 §4 三种模式组织：
> Mode 1（合体）、Mode 2（分体）、Mode 3（集群）+ K8s 一键部署。

## 目录结构

```
deploy/
├── docker-compose.yml              # Mode 1：合体（默认，最快上手）
├── docker-compose.split.yml        # Mode 2：分体（前端独立部署）
├── docker-compose.cluster.yml      # Mode 3：集群（1 前端 + N 后端）
├── Dockerfile.backend              # 后端 jar 镜像（Mode 1/2/3 共用）
├── Dockerfile.frontend             # 前端 nginx 镜像（Mode 2/3 用）
├── nginx.conf.template             # nginx 配置模板（envsubst 注入 BACKEND_SERVICE）
├── Makefile                        # make dev/split/cluster/build/k8s-apply/clean
├── bin/
│   ├── build-images.sh             # 一键构建两个镜像（支持 --push）
│   ├── start-mode1.sh              # Mode 1 启动
│   ├── start-mode2.sh              # Mode 2 启动
│   ├── start-mode3.sh [N]          # Mode 3 启动（默认 N=2）
│   └── k8s-apply.sh                # K8s 一键部署（占位符渲染 + kubectl apply）
├── env/
│   └── .env.example                # 环境变量模板（.env 自己创建，gitignore）
└── k8s/
    ├── 00-namespace.yaml           # Namespace + ServiceAccount + RBAC
    ├── 01-deployment-backend.yaml  # 后端 Deployment（replicas=2，HPA 友好）
    ├── 02-deployment-frontend.yaml # 前端 Deployment（replicas=1）
    ├── 03-service-backend.yaml     # 后端 ClusterIP Service
    ├── 04-service-frontend.yaml    # 前端 ClusterIP Service
    └── 05-ingress.yaml             # Ingress（HTTPS + 自动证书）
```

## 三种模式对比

| 模式 | 启动命令 | 容器数 | 访问地址 | 适用场景 |
|---|---|---|---|---|
| Mode 1 合体 | `make dev` | 1 | http://localhost:8080/ctc | 试用 / PoC / 个人开发 |
| Mode 2 分体 | `make split` | 2 | http://localhost | 前端频繁迭代 / CDN |
| Mode 3 集群 | `make cluster N=3` | 1+N | http://localhost | 生产 / 多副本高可用 |

## 快速上手

```bash
# 1. 启动 Mode 1
cd deploy
make dev

# 2. 浏览器访问
open http://localhost:8080/ctc

# 3. 看日志
make logs

# 4. 停止
make down
```

## 构建 + 推送镜像

```bash
# 仅构建（本地测试用）
make build

# 构建并 push 到 ghcr.io
make push
```

## K8s 部署

```bash
cd deploy
make k8s-apply INGRESS_DOMAIN=ctc.example.com
```

前置：
- 已有 k8s/k3s 集群 + kubectl 已配置
- 镜像已 push 到 OCI_REGISTRY（默认 ghcr.io/yuku123）
- 占位符：
  - `NAMESPACE`：默认 `z-ctc`
  - `INGRESS_DOMAIN`：必填（如 `ctc.example.com`）
  - `IMAGE_VERSION`：默认 `1.0.1`
  - `OCI_REGISTRY`：默认 `ghcr.io/yuku123`

## 占位符渲染

k8s/*.yaml 用 `{{XXX}}` 占位符（envsubst 风格），`bin/k8s-apply.sh` 渲染后 apply：

```bash
# 手动渲染查看
for f in k8s/*.yaml; do
    NAMESPACE=z-ctc INGRESS_DOMAIN=ctc.example.com \
    IMAGE_VERSION=1.0.1 OCI_REGISTRY=ghcr.io/yuku123 \
    envsubst < "$f" > "${f%.yaml}.rendered.yaml"
done
```

## 数据库凭证

- **本地试用**：默认用 H2 内存模式（重启数据丢失），环境变量里密码留空
- **生产 / K8s**：用 MySQL/PostgreSQL，密码通过 K8s Secret 注入
  ```bash
  kubectl -n z-ctc create secret generic z-ctc-db-credentials \
      --from-literal=password='YOUR_PASSWORD'
  ```

## 与 lead 部署文档的关系

按规范 §4.5，本目录是**单中间件**部署模板；lead/006_部署方案 是**全集**部署编排。
本目录落地后，lead/006 应反向引用本仓 `deploy/` 而非重复维护 yaml。

## 许可

MIT