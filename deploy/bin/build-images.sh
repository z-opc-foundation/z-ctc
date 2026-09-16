#!/usr/bin/env bash
# 一键构建 z-ctc 后端 + 前端镜像
#
# 前置：
#   - mvn 已编出 admin exec jar
#   - 前端 dist/ 已产出（admin pom 在 package 阶段自动触发）
#   - docker 已登录 ghcr.io（push 时需要）
#
# 用法：
#   bash bin/build-images.sh             # 构建本地镜像（不 push）
#   bash bin/build-images.sh --push      # 构建并 push 到 ghcr.io
#   bash bin/build-images.sh --no-cache  # 强制重新拉 base image

set -euo pipefail

ROOT="$(cd "$(dirname "$ 0")/.." && pwd)"
cd "$ROOT"

PUSH=false
NO_CACHE=""
IMAGE_VERSION="${IMAGE_VERSION:-1.0.1}"
OCI_REGISTRY="${OCI_REGISTRY:-ghcr.io/yuku123}"

for arg in "$ @"; do
    case "$arg" in
        --push) PUSH=true ;;
        --no-cache) NO_CACHE="--no-cache" ;;
    esac
done

echo "=== Step 1/3: 编 Java admin jar（含前端 V2 自动注入）==="
mvn -B -DskipTests -pl z-ctc-admin -am package

echo ""
echo "=== Step 2/3: 构建后端镜像（$ OCI_REGISTRY/z-ctc-admin:$ IMAGE_VERSION）==="
docker build $NO_CACHE \
    -f deploy/Dockerfile.backend \
    --build-arg JAR_FILE=z-ctc-admin/target/z-ctc-admin-1.0.1-exec.jar \
    -t "$ OCI_REGISTRY/z-ctc-admin:$ IMAGE_VERSION" \
    .

echo ""
echo "=== Step 3/3: 构建前端镜像（$ OCI_REGISTRY/z-ctc-frontend:$ IMAGE_VERSION）==="
docker build $NO_CACHE \
    -f deploy/Dockerfile.frontend \
    -t "$ OCI_REGISTRY/z-ctc-frontend:$ IMAGE_VERSION" \
    .

if [ "$ PUSH" = "true" ]; then
    echo ""
    echo "=== Pushing to $ OCI_REGISTRY ==="
    docker push "$ OCI_REGISTRY/z-ctc-admin:$ IMAGE_VERSION"
    docker push "$ OCI_REGISTRY/z-ctc-frontend:$ IMAGE_VERSION"
fi

echo ""
echo "✓ 构建完成。本地镜像："
docker images | grep z-ctc || true