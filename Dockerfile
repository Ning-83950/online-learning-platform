# 多阶段构建：先编译 Spring Boot 项目，再运行
FROM maven:3.8-eclipse-temurin-8 AS builder

WORKDIR /build
COPY server/pom.xml .
COPY server/src ./src

# 跳过测试，打包可执行 jar
RUN mvn -DskipTests package

# 运行阶段：只保留 JRE 和 jar
FROM eclipse-temurin:8-jdk

WORKDIR /app
COPY --from=builder /build/target/online-learning-server-1.0.0.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
