# =====================================================================
# 若依后端镜像（多阶段构建）
# ---------------------------------------------------------------------
# 构建上下文 = backend/ 目录（见 deploy/docker-compose.yml 的 context: ../backend）
# 产物：/app/ruoyi-admin.jar
#   backend/ruoyi-admin/pom.xml 的 <finalName>${project.artifactId}</finalName>
#   + spring-boot-maven-plugin repackage => ruoyi-admin/target/ruoyi-admin.jar
#
# 手动构建（在 deploy/ 目录下）：
#   docker build -f backend.Dockerfile -t ruoyi-edu/backend:3.9.2 ../backend
# =====================================================================

# ---------------------------------------------------------------------
# 阶段 1：编译打包
# ---------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# java.version 在 pom 里写的是 17（若依 v3.9.2 原始值），用 JDK 21 编译 source/target 17，
# 运行时仍在 Java 21 JRE 上，符合 versions.md「兼容性说明」。

# ---- 依赖缓存层：只复制 POM，源码变了也不会重下依赖 ----
COPY pom.xml ./
COPY ruoyi-admin/pom.xml     ruoyi-admin/
COPY ruoyi-common/pom.xml    ruoyi-common/
COPY ruoyi-framework/pom.xml ruoyi-framework/
COPY ruoyi-system/pom.xml    ruoyi-system/
COPY ruoyi-quartz/pom.xml    ruoyi-quartz/
COPY ruoyi-generator/pom.xml ruoyi-generator/
COPY ruoyi-education/pom.xml ruoyi-education/

# 预热 ~/.m2。reactor 内部模块（com.ruoyi:*）此时还没有 jar，
# 用 excludeGroupIds 跳过它们；即使仍然失败也不影响下面的真实构建（故 || true）。
RUN mvn -q -B -DskipTests -DexcludeGroupIds=com.ruoyi dependency:go-offline || true

# ---- 真实构建 ----
COPY . .
RUN mvn -q -B -DskipTests package

# ---------------------------------------------------------------------
# 阶段 2：运行时（精简 JRE，非 root 用户）
# ---------------------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# curl：用于 HEALTHCHECK；tzdata：让 TZ=Asia/Shanghai 生效
# 非 root 用户：uid/gid 10001（避开 Ubuntu 基础镜像自带的 ubuntu:1000）
RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends curl tzdata \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system --gid 10001 ruoyi \
 && useradd --system --uid 10001 --gid ruoyi --home-dir /home/ruoyi --shell /usr/sbin/nologin ruoyi \
 && mkdir -p /app /home/ruoyi/logs /app/data/uploadPath \
 && chown -R ruoyi:ruoyi /app /home/ruoyi

WORKDIR /app

# addResources=true + repackage 已把 resources 打进 jar，这里只需要一个 jar
COPY --from=build --chown=ruoyi:ruoyi /build/ruoyi-admin/target/ruoyi-admin.jar /app/ruoyi-admin.jar

USER ruoyi

EXPOSE 8080

# 本应用没有引入 spring-boot-starter-actuator（backend/pom.xml、ruoyi-admin/pom.xml
# 都没有 actuator 依赖，application.yml 里也没有 management.* 配置），
# 所以不做 /actuator/health 探测。
# 改用 SecurityConfig 中 permitAll 的匿名接口 /captchaImage：
#   backend/ruoyi-framework/.../config/SecurityConfig.java:103
# 它返回 200 + JSON，且不依赖数据库写入，是可靠的存活探针。
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=10 \
    CMD curl -fsS http://127.0.0.1:8080/captchaImage || exit 1

# shell 形式是为了展开 $JAVA_OPTS；exec 保证 java 是 PID 1 的进程（能正确收到 SIGTERM）
ENTRYPOINT ["/bin/sh", "-c", "exec java $JAVA_OPTS -Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8 -jar /app/ruoyi-admin.jar"]
