#!/bin/bash
set -e  # 脚本遇到错误立即退出，避免后续无效操作

# ====================== 配置项（请根据你的项目修改）======================
PROJECT_NAME="zb-ctc"       # 项目名称（用于镜像/容器命名）
DOCKERFILE_PATH="./Dockerfile"      # Dockerfile 路径
IMAGE_NAME=":latest" # 镜像名称+标签
CONTAINER_NAME=""    # 容器名称
HOST_PORT=8092                      # 宿主机映射端口
CONTAINER_PORT=8080                 # 容器内端口（对应SpringBoot服务端口）
HEALTH_CHECK_URL="http://127.0.0.1:/actuator/health" # 健康检查接口（需开启SpringBoot Actuator）
HEALTH_CHECK_TIMEOUT=30             # 健康检查超时时间（秒）
# =========================================================================

# 函数：打印带时间戳的日志
log() {
    echo "[2026-04-11 19:21:36] $1"
}

# 函数：检查Docker是否运行
check_docker_status() {
    log "检查Docker服务状态..."
    if ! docker info >/dev/null 2>&1; then
        log "错误：Docker服务未运行，请先启动Docker！"
        exit 1
    fi
    log "Docker服务运行正常"
}

# 函数：打包
package() {
  sh package.sh
}


# 函数：构建Docker镜像
build_image() {
    log "开始构建Docker镜像: ${IMAGE_NAME}"
    docker build -f "${DOCKERFILE_PATH}" -t "${IMAGE_NAME}" .
    log "镜像构建完成: $(docker images -q ${IMAGE_NAME})"
}

# 函数：检查旧容器是否存在
check_old_container() {
    OLD_CONTAINER_ID=$(docker ps -aq --filter "name=${CONTAINER_NAME}")
    if [ -n "${OLD_CONTAINER_ID}" ]; then
        log "检测到旧容器存在（ID: ${OLD_CONTAINER_ID}），准备平滑替换"
        # 给旧容器加临时后缀，避免名称冲突
        docker rename "${CONTAINER_NAME}" "${CONTAINER_NAME}-old"
        OLD_CONTAINER_NAME="${CONTAINER_NAME}-old"
    else
        log "未检测到旧容器，将直接启动新容器"
        OLD_CONTAINER_NAME=""
    fi
}

# 函数：启动新容器
start_new_container() {
    log "启动新容器: ${CONTAINER_NAME}"
    # 启动新容器（端口映射、后台运行、自动重启）
    docker run -d         --name "${CONTAINER_NAME}"         -p "${HOST_PORT}:${CONTAINER_PORT}"         --restart=always         "${IMAGE_NAME}"

    NEW_CONTAINER_ID=$(docker ps -aq --filter "name=${CONTAINER_NAME}")
    log "新容器已启动（ID: ${NEW_CONTAINER_ID}）"
}

# 函数：健康检查（验证新容器服务是否可用）
health_check() {
    log "开始健康检查，超时时间：${HEALTH_CHECK_TIMEOUT}秒"
    local timeout=0
    while [ ${timeout} -lt ${HEALTH_CHECK_TIMEOUT} ]; do
        # 尝试访问健康检查接口
        if curl -s -f "${HEALTH_CHECK_URL}" >/dev/null 2>&1; then
            log "健康检查通过！新容器服务可用"
            return 0
        fi
        timeout=$((timeout + 1))
        sleep 1
    done
    # 健康检查失败，回滚操作
    log "错误：新容器健康检查超时，服务不可用！"
    log "开始回滚：停止并删除新容器，恢复旧容器"
    docker stop "${CONTAINER_NAME}" >/dev/null 2>&1
    docker rm "${CONTAINER_NAME}" >/dev/null 2>&1
    if [ -n "${OLD_CONTAINER_NAME}" ]; then
        docker rename "${OLD_CONTAINER_NAME}" "${CONTAINER_NAME}"
        docker start "${CONTAINER_NAME}" >/dev/null 2>&1
        log "回滚完成，旧容器已恢复运行"
    fi
    exit 1
}

# 函数：清理旧容器
clean_old_container() {
    if [ -n "${OLD_CONTAINER_NAME}" ]; then
        log "停止并删除旧容器: ${OLD_CONTAINER_NAME}"
        docker stop "${OLD_CONTAINER_NAME}" >/dev/null 2>&1
        docker rm "${OLD_CONTAINER_NAME}" >/dev/null 2>&1
        log "旧容器已清理完成"
    fi
}

# 主执行流程
main() {
    log "===================== 开始部署 SpringBoot 服务 ====================="
    check_docker_status
    package
    build_image
    check_old_container
    start_new_container
    health_check
    clean_old_container
    log "===================== 部署完成！服务已平滑更新 ====================="
    log "当前运行容器信息："
    docker ps --filter "name=${CONTAINER_NAME}" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
}

# 执行主流程
main
