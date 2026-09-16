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
    DB_HOST=$(echo $SPRING_DATASOURCE_URL | sed -n 's/.*@\([^:]*\):.*/\1/p')
    DB_PORT=$(echo $SPRING_DATASOURCE_URL | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')

    if [ -z "$DB_PORT" ]; then
        DB_PORT="3306"
    fi

    log "Waiting for database at ${DB_HOST}:${DB_PORT}..."

    # 等待数据库就绪
    for i in {1..30}; do
        if nc -z $DB_HOST $DB_PORT; then
            log "Database is ready!"
            break
        fi
        log "Waiting for database... ($i/30)"
        sleep 2
    done
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
