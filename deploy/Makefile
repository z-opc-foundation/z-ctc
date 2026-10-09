# Z-CTC deploy/ 顶层 Makefile
# 提供三种部署模式的入口 + 通用 build/k8s/clean 任务
#
# 常用命令：
#   make dev       # Mode 1 合体（最快上手）
#   make split     # Mode 2 分体（前端独立部署）
#   make cluster N=3  # Mode 3 集群（3 副本后端）
#   make build     # 构建两个镜像
#   make k8s-apply INGRESS_DOMAIN=ctc.example.com  # K8s 一键部署
#   make clean     # 停掉 + 删容器 + 删镜像

# ---- 配置 ----
IMAGE_VERSION ?= 1.0.1
OCI_REGISTRY ?= ghcr.io/yuku123
NAMESPACE ?= z-ctc
INGRESS_DOMAIN ?= ctc.example.com

COMPOSE_BASE = docker compose
COMPOSE_SPLIT = docker compose -f docker-compose.split.yml
COMPOSE_CLUSTER = docker compose -f docker-compose.cluster.yml

# ---- 入口命令 ----
.PHONY: help dev split cluster build k8s-apply clean ps logs down

help: ## 显示帮助
	@echo "Z-CTC 部署命令："
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'
	@echo ""

dev: ## Mode 1：合体（单容器，jar 内嵌前端）
	bash bin/start-mode1.sh

split: ## Mode 2：分体（前端 nginx + 后端 jar）
	bash bin/start-mode2.sh

cluster: ## Mode 3：集群（1 前端 + N=2 后端，可用 N=5 调整）
	bash bin/start-mode3.sh $(N)

build: ## 构建后端 + 前端镜像（本地，不 push）
	bash bin/build-images.sh

push: ## 构建并 push 到 ghcr.io
	bash bin/build-images.sh --push

k8s-apply: ## K8s 一键部署（需设置 INGRESS_DOMAIN）
	INGRESS_DOMAIN=$(INGRESS_DOMAIN) NAMESPACE=$(NAMESPACE) IMAGE_VERSION=$(IMAGE_VERSION) OCI_REGISTRY=$(OCI_REGISTRY) \
		bash bin/k8s-apply.sh

ps: ## 查看运行中的容器
	@echo "=== Mode 1 ==="
	$(COMPOSE_BASE) ps 2>/dev/null || echo "(未运行)"
	@echo ""
	@echo "=== Mode 2 ==="
	$(COMPOSE_SPLIT) ps 2>/dev/null || echo "(未运行)"
	@echo ""
	@echo "=== Mode 3 ==="
	$(COMPOSE_CLUSTER) ps 2>/dev/null || echo "(未运行)"

logs: ## 查看 Mode 1 日志
	$(COMPOSE_BASE) logs -f z-ctc-admin

down: ## 停掉所有模式
	$(COMPOSE_BASE) down 2>/dev/null || true
	$(COMPOSE_SPLIT) down 2>/dev/null || true
	$(COMPOSE_CLUSTER) down 2>/dev/null || true

clean: ## 停掉 + 删本地镜像
	make down
	docker rmi $(OCI_REGISTRY)/z-ctc-admin:$(IMAGE_VERSION) 2>/dev/null || true
	docker rmi $(OCI_REGISTRY)/z-ctc-suit:$(IMAGE_VERSION) 2>/dev/null || true

# 详细模式默认不显示命令本身
.SILENT: help ps logs