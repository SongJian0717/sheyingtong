# ---- 构建阶段：Maven 打包 fat jar ----
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 先只拷 pom 拉依赖，利用 Docker 层缓存避免每次全量下载
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

# 再拷源码编译打包（-DskipTests：质量门禁在 CI，容器内只负责出产物）
COPY . .
RUN mvn -B -DskipTests clean package

# ---- 运行阶段 ----
# 用 Ubuntu 版 JRE（不用 alpine/musl），照片打分链路要用 java.awt 做图片压缩，
# 需要完整 glibc + fontconfig，这两样在 temurin-ubuntu 里配齐（alpine 版易缺字体报错）
FROM eclipse-temurin:17-jre
WORKDIR /app

# fontconfig + 中文字体：AWT 渲染图片/水印必需；curl 给容器健康检查用
RUN apt-get update \
    && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/target/*.jar /app/app.jar

ENV TZ=Asia/Shanghai
EXPOSE 8080

# headless：服务器无图形环境，AWT 必须显式 headless
# tmpdir /tmp：50MB 上传的原图先落临时目录再处理，放容器小磁盘易占满
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-Djava.io.tmpdir=/tmp", "-Duser.timezone=Asia/Shanghai", "-jar", "/app/app.jar"]