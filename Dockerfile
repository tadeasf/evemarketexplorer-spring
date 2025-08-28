FROM gradle:8.10-jdk21 AS build

WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src

RUN gradle build --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring

RUN mkdir -p /app/db && chown -R spring:spring /app/db

COPY --from=build --chown=spring:spring /app/build/libs/*.jar app.jar

USER spring:spring

EXPOSE 13921

ENTRYPOINT ["java", "-jar", "app.jar"]