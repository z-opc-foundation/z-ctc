#!/bin/bash
set -e

# 日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

log "Starting CTC Application..."

# 检查环境变量
if [ -z "$SPRING_DATASOURCE_URL" ]; then
    log "Warning: SPRING_DATASOURCE_URL is not set, using default configuration"
fi

# 等待数据库连接（如果配置了数据库）
if [ -n "$SPRING_DATASOURCE_URL" ]; then
    # 从 JDBC URL 提取主机和端口
    # ⚠ URL 有两种形态，原写法只认带 @ 的那种：
    #   带账号：jdbc:mysql://user:pass@host:port/db?...
    #   不带账号：jdbc:mysql://host:port/db?...   ← 生产用的就是这种
    #   原正则 s/.*@\([^:]*\):.*/\1/p 对第二种**匹配不到** ⇒ DB_HOST 为空
    #   ⇒ 等待逻辑一直在连空主机（静默失效，不报错）。
    DB_HOST=$(echo "$SPRING_DATASOURCE_URL" | sed -n 's|^jdbc:[^:]*://[^@/]*@\([^:/]*\).*|\1|p')
    if [ -z "$DB_HOST" ]; then
        DB_HOST=$(echo "$SPRING_DATASOURCE_URL" | sed -n 's|^jdbc:[^:]*://\([^:/]*\).*|\1|p')
    fi
    DB_PORT=$(echo "$SPRING_DATASOURCE_URL" | sed -n 's|.*:\([0-9]*\)/.*|\1|p')

    if [ -z "$DB_HOST" ]; then
        log "Warning: cannot parse host from SPRING_DATASOURCE_URL, skip database wait"
    fi

    if [ -n "$DB_HOST" ]; then
        if [ -z "$DB_PORT" ]; then
            DB_PORT="3306"
        fi

        log "Waiting for database at ${DB_HOST}:${DB_PORT}..."

        # 等待数据库就绪
        # ⚠ 用 bash 内建的 /dev/tcp 探端口，**不用 nc** ——
        #   镜像（eclipse-temurin:8-jdk）只装了 tzdata 和 wget，**没有 netcat**
        #   ⇒ 原来的 `nc -z` 是 command not found ⇒ 每次都失败 ⇒ 等 30×2s
        #   之后照样启动，DB 等待等于没做（而且不报错）。
        #   /dev/tcp 是 bash 内建 ⇒ 不用往镜像里加包。
        for i in {1..30}; do
            if (exec 3<>/dev/tcp/"$DB_HOST"/"$DB_PORT") 2>/dev/null; then
                log "Database is ready!"
                break
            fi
            log "Waiting for database... ($i/30)"
            sleep 2
        done
    fi
fi

# 设置 JVM 参数
if [ -z "$JAVA_OPTS" ]; then
    JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/heapdump.hprof"
fi

# 设置 Spring Boot 参数
if [ -z "$SPRING_OPTS" ]; then
    SPRING_OPTS="--logging.file=/app/logs/application.log --logging.level.root=INFO"
fi

log "JAVA_OPTS: $JAVA_OPTS"
log "SPRING_OPTS: $SPRING_OPTS"

# 启动应用
log "Starting Java application..."
exec java $JAVA_OPTS -jar /app/app.jar $SPRING_OPTS "$@"
