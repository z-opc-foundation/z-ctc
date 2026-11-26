#!/usr/bin/env bash
# k8s 一键部署（占位符渲染 + kubectl apply）
# 占位符（envsubst ${XXX} 风格）：
#   NAMESPACE       - 默认 z-ctc
#   INGRESS_DOMAIN  - 必填，如 ctc.example.com
#   TLS_SECRET_NAME - 默认 z-ctc-tls
#   OCI_REGISTRY    - 默认 ghcr.io/yuku123
#   IMAGE_VERSION   - 默认 1.0.1

set -euo pipefail

cd "$(dirname "$ 0")/.."

export NAMESPACE="${NAMESPACE:-z-ctc}"
export INGRESS_DOMAIN="${INGRESS_DOMAIN:?必须设置 INGRESS_DOMAIN（如 ctc.example.com）}"
export TLS_SECRET_NAME="${TLS_SECRET_NAME:-z-ctc-tls}"
export OCI_REGISTRY="${OCI_REGISTRY:-ghcr.io/yuku123}"
export IMAGE_VERSION="${IMAGE_VERSION:-1.0.1}"

echo "=== 渲染占位符 ==="
RENDERED_DIR="$(mktemp -d)"
for f in k8s/*.yaml; do
    out="$ RENDERED_DIR/$(basename "$ f")"
    envsubst < "$ f" > "$ out"
    echo "  rendered: $ out"
done

echo ""
echo "=== kubectl apply ==="
kubectl apply -f "$ RENDERED_DIR/00-namespace.yaml"
kubectl apply -f "$ RENDERED_DIR/01-deployment-backend.yaml"
kubectl apply -f "$ RENDERED_DIR/02-deployment-frontend.yaml"
kubectl apply -f "$ RENDERED_DIR/03-service-backend.yaml"
kubectl apply -f "$ RENDERED_DIR/04-service-frontend.yaml"
kubectl apply -f "$ RENDERED_DIR/05-ingress.yaml"

echo ""
echo "=== 等 pod Ready ==="
kubectl -n "$ NAMESPACE" get pods -w

echo ""
echo "✓ 部署完成。访问：https://schedule.$ INGRESS_DOMAIN"

rm -rf "$ RENDERED_DIR"