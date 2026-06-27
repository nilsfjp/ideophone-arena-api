# syntax=docker/dockerfile:1

# Build stage: Temurin-21 + Maven. Use the project's Maven wrapper to build the jar.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy the wrapper and project sources, then build the jar.
# Tests run via ./mvnw test, not in the image build.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN ./mvnw -q -DskipTests package

# Runtime stage: JRE only, no build tooling.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as a non-root user.
RUN groupadd --system arena && useradd --system --gid arena --no-create-home arena

# Copy the single Spring Boot jar produced by the build stage.
COPY --from=build /build/target/*.jar /app/app.jar

USER arena
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
