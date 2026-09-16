#!/usr/bin/env bash
# Mode 3 集群启动：1 前端 nginx + N 后端 jar（默认 N=2，可用 --scale 调整）
# 详见 deploy/docker-compose.cluster.yml

set -euo pipefail

cd "$(dirname "$ 0")/.."

REPLICAS="${1:-2}"

echo "=== 启动 Mode 3（集群，backend replicas=$ REPLICAS）==="
docker compose -f docker-compose.cluster.yml up -d --scale z-ctc-backend="$ REPLICAS"

echo ""
echo "✓ 启动完成。"
docker compose -f docker-compose.cluster.yml ps
echo ""
echo "  访问前端：http://localhost"
echo "  验证反代：curl http://localhost/api/actuator/health"
echo "  调整副本：bash bin/start-mode3.sh 5（启动 5 个后端）"
echo "  停止：docker compose -f docker-compose.cluster.yml down"