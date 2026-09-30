FROM rollcall-backend-runtime:17

WORKDIR /app
USER root
RUN apk add --no-cache openjdk17-jre fontconfig ttf-dejavu
COPY blade-api.jar /app/blade-api.jar

ENV TZ=Asia/Shanghai
ENV JAVA_OPTS=""
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java --add-opens=java.base/java.lang=ALL-UNNAMED $JAVA_OPTS -Duser.timezone=Asia/Shanghai -jar /app/blade-api.jar --spring.profiles.active=dev"]
