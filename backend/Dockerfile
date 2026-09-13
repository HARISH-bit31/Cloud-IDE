# Stage 1: Build stage with Maven and OpenJDK 21
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and package JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Lightweight runtime stage with Eclipse Temurin JRE 21
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create unprivileged system user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy compiled JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership and switch to non-root user
RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8088

ENV JAVA_OPTS="-Xms64m -Xmx512m -XX:+UseG1GC -XX:TieredStopAtLevel=1"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
