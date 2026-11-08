# syntax=docker/dockerfile:1
FROM node:24-alpine AS frontend
WORKDIR /web
COPY frontend/package*.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /build
COPY backend/pom.xml backend/pom.xml
RUN mvn -B -f backend/pom.xml dependency:go-offline
COPY backend/src/ backend/src/
COPY --from=frontend /web/dist/ frontend/dist/
# Database integration tests run separately against PostgreSQL, not during image builds.
RUN mvn -B -f backend/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* && groupadd -r campushub && useradd -r -g campushub campushub
WORKDIR /app
COPY --from=backend --chown=campushub:campushub /build/backend/target/campushub-1.0.0.jar app.jar
USER campushub
ENV PORT=8080 JAVA_TOOL_OPTIONS="-Xms64m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=32m -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 CMD curl -fsS "http://127.0.0.1:${PORT}/api/health" || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
