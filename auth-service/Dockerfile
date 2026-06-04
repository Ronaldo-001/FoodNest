# ─────────────────────────────────────────────────────────────
# Stage 1: Build
# ─────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build
COPY pom.xml .
# Download dependencies first (layer cache)
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ─────────────────────────────────────────────────────────────
# Stage 2: Runtime (non-root, minimal image)
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
 
#Health check need s curl to check the endpoint
RUN apk add --no-cache curl
# Create non-root user
RUN addgroup -S foodwise && adduser -S foodwise -G foodwise

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

# Ensure jar is owned by non-root user
RUN chown foodwise:foodwise app.jar

USER foodwise

EXPOSE 8081

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
