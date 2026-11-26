#!/usr/bin/env bash
# Mode 2 分体启动：前端 nginx + 后端 jar 两个容器
# 详见 deploy/docker-compose.split.yml

set -euo pipefail

cd "$(dirname "$ 0")/.."

echo "=== 启动 Mode 2（分体）==="
docker compose -f docker-compose.split.yml up -d

echo ""
echo "✓ 启动完成。"
echo "  访问前端：http://localhost"
echo "  访问后端（需 exec 进容器）：docker exec -it z-ctc-backend curl http://127.0.0.1:8080/ctc/actuator/health"
echo "  验证反代：curl http://localhost/api/actuator/health"
echo "  停止：docker compose -f docker-compose.split.yml down"