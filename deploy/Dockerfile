# z-ctc-admin 生产镜像（卡 TASK-20261023-008）
#
# 【为什么基底是「现役镜像」而不是 jre 基础镜像】
#   仓根那个 `dockerfile` 走 `FROM eclipse-temurin:8-jdk` + `RUN apt-get install tzdata wget`：
#   ① 本机 Docker Hub 不可达（基座只能走镜像站，而镜像站名字写死在 FROM 里 ⇒ 改它就是改仓）；
#   ② 本机是 arm64，amd64 的 apt 步骤只能靠模拟执行，慢且不可复现。
#   线上 z-ctc-admin:1.0.5-authn-1 就是那份 `dockerfile` 的产物
#   （Temurin jdk8u504 / JAVA_HOME=/opt/java/openjdk / ENTRYPOINT /app/docker-entrypoint.sh /
#     /app/logs 已就位），已在线上验证过。
#   本轮只换 jar ⇒ 基底直接取现役镜像 ⇒ 运行时**零漂移**，把变量收敛到「jar 内容」一个。
#
# 【构建】
#   cd z-ctc
#   mvn -B -pl z-ctc-admin -am clean package -Dmaven.test.skip=true
#   docker buildx build --platform linux/amd64 --provenance=false --sbom=false --load \
#     -f deploy/Dockerfile -t z-ctc-admin:1.0.5-zboot112 .
#
# 【tag 铁律】
#   产出的 tag 必须与 deploy/k8s/06-deployment-family.yaml 的 `image:` 逐字一致 ——
#   本集群 imagePullPolicy=Never，k3s 按字面引用去节点本地 content store 找。
FROM z-ctc-admin:1.0.5-authn-1

# Spring Boot 可执行 fat jar（只匹配 exec 胖包，普通 jar 不匹配）
COPY z-ctc-admin/target/z-ctc-admin-*-exec.jar /app/app.jar
