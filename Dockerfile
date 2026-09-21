# --platform=$BUILDPLATFORM : builder 스테이지는 "빌드 호스트" 아치(GitHub 러너 = amd64)에서 네이티브로 실행.
# c7g(arm64) 측정용 이미지를 buildx --platform linux/arm64 로 만들 때, 이 줄이 없으면 Gradle/JVM 전체가
# QEMU 에뮬레이션 위에서 돌아 빌드가 수 배 느려지고 OOM/타임아웃 위험이 생김.
# JAR는 아치 중립 바이트코드라 amd64에서 빌드해 arm64 런타임에 넣어도 무방 (Docker 공식 "cross-compilation" 패턴).
FROM --platform=$BUILDPLATFORM gradle:8.6.0-jdk21 AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew

RUN ./gradlew dependencies

COPY src src

RUN ./gradlew bootjar

# 런타임 스테이지는 --platform 으로 지정한 "타깃" 아치를 따름 (linux/arm64 → aarch64 JRE).
# RUN 명령이 없어 에뮬레이션 비용 0. amd64 이미지를 arm64 EC2에서 QEMU로 돌리면 측정이 전부 무효이므로
# 배포 후 반드시 docker image inspect --format '{{.Architecture}}' 로 arm64 확인 (deploy.yml [arch check]).
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar ./app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]