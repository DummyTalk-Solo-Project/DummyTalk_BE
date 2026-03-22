FROM gradle:8.6.0-jdk21 as builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew

RUN ./gradlew dependencies

COPY src src

RUN ./gradlew bootjar

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar ./app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]

FROM docker.elastic.co/elasticsearch/elasticsearch:8.6.0

# nori 플러그인 설치 명령 실행
RUN elasticsearch-plugin install analysis-nori