# z-ctc-admin 生产镜像（卡 TASK-20261023-008）
#
# 【为什么基底是「现役镜像」而不是 jre 基础镜像】
#   仓根那个 `Dockerfile` 走 `FROM eclipse-temurin:17-jdk` + `RUN apt-get install tzdata wget`：
#   ① 本机 Docker Hub 不可达（基座只能走镜像站，而镜像站名字写死在 FROM 里 ⇒ 改它就是改仓）；
#   ② 本机是 arm64，amd64 的 apt 步骤只能靠模拟执行，慢且不可复现。
#   本轮只换 jar ⇒ 基底直接取现役镜像 ⇒ 运行时**零漂移**，把变量收敛到「jar 内容」一个。
#
# ⚠ 2026-11-06 基底换 1.0.5-authn-1 → 1.0.6-j17（Boot3 迁移遗留修）：
#   1.0.5-authn-1 是 **jdk8u504** 底（实测 `java -version` = 1.8.0_504），
#   而 z-ctc 已随 Boot3 火车迁到 JDK17（class major 61）⇒ 旧基底上跑新 jar 必崩：
#     UnsupportedClassVersionError (class file version 61.0, recognizes up to 52.0)
#   ⇒ 基底必须与产物同一档（17）。1.0.6-j17 已用 Temurin 17.0.20.1 重建，
#     其余运行时布局（JAVA_HOME / ENTRYPOINT /app/docker-entrypoint.sh /app/logs）不变。
#
# 【构建】
#   cd z-ctc
#   mvn -B -pl z-ctc-admin -am clean package -Dmaven.test.skip=true
#   docker buildx build --platform linux/amd64 --provenance=false --sbom=false --load \
#     -f deploy/Dockerfile -t z-ctc-admin:1.0.6-zboot113 .
#
# 【tag 铁律】
#   产出的 tag 必须与 deploy/k8s/06-deployment-family.yaml 的 `image:` 逐字一致 ——
#   本集群 imagePullPolicy=Never，k3s 按字面引用去节点本地 content store 找。
FROM z-ctc-admin:1.0.6-j17

# Spring Boot 可执行 fat jar（只匹配 exec 胖包，普通 jar 不匹配）
COPY z-ctc-admin/target/z-ctc-admin-*-exec.jar /app/app.jar
