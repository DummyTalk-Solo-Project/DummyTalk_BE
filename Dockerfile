FROM gradle:8.6.0-jdk21 as builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN ./gradlew dependencies

COPY src src

RUN chmod +x gradlew

RUN ./gradlew bootjar

FROM openjdk:21-jdk

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar ./app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]