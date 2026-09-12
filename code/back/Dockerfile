# ==================== 第一阶段：编译打包 ====================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

# 设置工作目录
WORKDIR /build

# 1. 先拷贝 Maven 配置文件和 pom.xml（利用 Docker 缓存机制加速构建）
COPY maven-setting.xml /root/.m2/settings.xml
COPY pom.xml .

# 2. 拷贝源码
COPY src ./src

# 3. 执行打包（会直接使用刚刚复制过去的 settings.xml）
RUN mvn clean package -Dmaven.test.skip=true

# ==================== 第二阶段：运行环境 ====================
FROM registry.sinoaopt.com/env/jdk:17-alpine

VOLUME /tmp

# 从第一阶段复制编译好的 jar 包
COPY --from=builder /build/target/whours-m001-api.jar /app.jar

# 换源 + 安装字体
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories && \
    apk add --no-cache font-adobe-100dpi ttf-dejavu fontconfig

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dnetworkaddress.cache.ttl=0 -Dnetworkaddress.cache.negative.ttl=0 -Djava.security.egd=file:/dev/./urandom -Duser.timezone=Asia/Shanghai -jar /app.jar"]
