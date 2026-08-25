FROM gradle:8-jdk21 AS build
WORKDIR /app

# 의존성만 먼저 캐싱 (build.gradle이 안 바뀌면 이 레이어 재사용 -> 매번 전체 재다운로드 안 함)
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon || true

COPY . .
RUN gradle build -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
