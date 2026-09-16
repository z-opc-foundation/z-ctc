# 基础镜像：Eclipse Temurin 8 JDK（支持amd64/arm64架构，M系列Mac可用）
# 历史原因：曾用 openjdk:8-jdk-slim，但 Docker Hub 已于 2022-07 停止更新/下架该镜像，
# 构建期会报 "docker.io/library/openjdk:8-jdk-slim: not found"。
# 见 FEATURE034 T2 关于 temurin 8-jdk → 8-jre-alpine 的体积优化规划（远期任务）。
FROM eclipse-temurin:8-jdk

# 维护者信息
LABEL maintainer="zifang"
LABEL description="CTC - 4A Center (Authentication, Account, Authorization, Audit)"

# 设置时区 + 装 wget (供 HEALTHCHECK 使用, debian 系用 apt-get)
RUN apt-get update && apt-get install -y --no-install-recommends tzdata wget && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apt-get remove -y tzdata && apt-get autoremove -y && apt-get clean

# 设定工作目录
WORKDIR /app

# 创建日志目录
RUN mkdir -p /app/logs

# 复制 Spring Boot 可执行 fat jar
# z-ctc-admin 走与 z-meta-admin 一致的双 jar 设计:
#   z-ctc-admin-1.0.0-SNAPSHOT.jar       -> library jar (被 z-opc-main-starter 引用)
#   z-ctc-admin-1.0.0-SNAPSHOT-exec.jar  -> Spring Boot 可执行 fat jar (本镜像使用)
COPY z-ctc-admin/target/z-ctc-admin-1.0.0-SNAPSHOT-exec.jar app.jar

# 复制启动脚本（数据库等待 + JVM 参数注入）
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

# 暴露端口
EXPOSE 8080

# 健康检查（FEATURE034 T4: 30s interval / 5s timeout / 60s start-period / 3 retries）
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -q -O- http://127.0.0.1:8080/doc.html || exit 1

# 启动命令
ENTRYPOINT ["/app/docker-entrypoint.sh"]
