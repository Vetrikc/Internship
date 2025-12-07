FROM gradle:8.5-jdk21 AS build
WORKDIR /app

COPY . .

RUN chmod +x gradlew

ARG MODULE_NAME

RUN ./gradlew :${MODULE_NAME}:bootJar -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

ARG MODULE_NAME
COPY --from=build /app/${MODULE_NAME}/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
