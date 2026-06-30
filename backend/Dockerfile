# syntax=docker/dockerfile:1

# =============================================================================
# Le Bar'app — backend image (multi-stage)
#
# Build stage : official Java 21 JDK + the repository Maven Wrapper (mvnw).
#   The wrapper is "only-script" (it self-downloads Maven 3.9.9), so the build
#   never depends on a locally installed Maven/Java. Tests are NOT run here:
#   the full Surefire + Failsafe suite is executed separately (./mvnw clean
#   verify) before building the image — see backend/docs/testing.md.
#
# Runtime stage : official Java 21 JRE (Alpine) running only the packaged jar,
#   as a non-root user. All configuration is supplied at runtime through
#   environment variables; no database credentials or JWT secret are baked in.
# =============================================================================

# ---- Build stage ----------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# Copy the Maven Wrapper + POM first so the dependency layer is cached and only
# re-resolved when pom.xml changes (not on every source edit).
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# Copy sources and package the executable (Spring Boot repackaged) jar.
COPY src/ src/
RUN ./mvnw -B clean package -DskipTests \
    && cp target/barapp-backend-*.jar /workspace/app.jar

# ---- Runtime stage --------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Run as a dedicated non-root user.
RUN addgroup -S barapp && adduser -S -G barapp -H barapp

# Only the executable jar reaches the runtime image (no Maven, sources or caches).
COPY --from=build --chown=barapp:barapp /workspace/app.jar /app/app.jar

USER barapp

# Internal application port (fixed; the host port is mapped by Compose).
EXPOSE 8080

# Container healthcheck against an existing public endpoint. BusyBox `wget`
# ships with the Alpine JRE image, so no extra runtime dependency is required.
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 \
    CMD wget -q -O - http://localhost:8080/api/menu >/dev/null 2>&1 || exit 1

# Exec form: the JVM is PID 1 and receives signals directly. Extra JVM tuning
# can be supplied at runtime via JAVA_TOOL_OPTIONS without rebuilding the image.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
