# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package \
    && cp target/tourisme-backend-*.jar /app/application.jar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# curl: healthcheck; postgresql-client: admin pg_dump backup from the API
RUN apk add --no-cache curl postgresql-client
COPY --from=build /app/application.jar /app/application.jar
RUN mkdir -p /app/uploads
ENV UPLOAD_DIR=/app/uploads
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=5 \
    CMD curl -fsS http://127.0.0.1:8081/api/activities -o /dev/null
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
