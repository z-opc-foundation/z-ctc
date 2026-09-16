#!/usr/bin/env bash
# Mode 1 合体启动：单容器，后端 jar 内嵌前端
# 详见 deploy/docker-compose.yml

set -euo pipefail

cd "$(dirname "$ 0")/.."

echo "=== 启动 Mode 1（合体）==="
docker compose up -d

echo ""
echo "✓ 启动完成。"
echo "  访问：http://localhost:8080/ctc"
echo "  日志：docker compose logs -f z-ctc-admin"
echo "  停止：docker compose down"