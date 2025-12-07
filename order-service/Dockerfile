FROM gradle:8.10-jdk21 AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
COPY build.gradle .
COPY */build.gradle .

RUN chmod +x ./gradlew
RUN ./gradlew build -x test --no-daemon || true

COPY . .

ARG MODULE_NAME=order-service
RUN ./gradlew :${MODULE_NAME}:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ARG MODULE_NAME=order-service
COPY --from=build /app/${MODULE_NAME}/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]