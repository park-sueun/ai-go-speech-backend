# === 1단계: 빌드 ===
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# 의존성 캐시 최적화 (소스보다 먼저 복사)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle .
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스 복사 및 빌드
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# === 2단계: 실행 (Playwright Chromium 포함) ===
FROM mcr.microsoft.com/playwright/java:v1.50.0-noble
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]