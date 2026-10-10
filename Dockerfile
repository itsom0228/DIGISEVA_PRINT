# ========================================================
# DIGISEVA PRINT - Multi-Stage Dockerfile for Render / Cloud Deployment
# ========================================================

# Stage 1: Build with Maven & OpenJDK 17
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /workspace

# Copy root & submodule POM files
COPY pom.xml .
COPY print-with-ai/pom.xml print-with-ai/

# Pre-fetch dependencies
RUN mvn dependency:go-offline -B -pl print-with-ai -am || true

# Copy application source
COPY print-with-ai print-with-ai

# Package application JAR
RUN mvn clean package -DskipTests -pl print-with-ai -am

# Stage 2: Lightweight JRE Runtime with Graphics/Font Libraries
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install font and graphics dependencies for PDF/QR rendering on Linux Alpine
RUN apk add --no-cache fontconfig freetype ttf-dejavu

# Create upload directory for documents
RUN mkdir -p /app/uploads && chmod 777 /app/uploads

# Copy generated JAR from build stage
COPY --from=build /workspace/print-with-ai/target/*.jar app.jar

# Default Port (Render sets $PORT dynamically)
EXPOSE 8080

# Start DigiSeva Print Spring Boot Application in Headless Cloud Mode
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=demo", "-jar", "app.jar"]
