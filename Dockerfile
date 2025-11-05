# Run this project with Java 17

# ---------- Stage 1: Build ----------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only pom first to leverage layer caching for dependencies
COPY pom.xml ./
RUN mvn -B dependency:go-offline

# Copy source and build the JAR
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Stage 2: Run ----------
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# Copy the single built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (Render default is 8080)
EXPOSE 8080

# Use a non-root user
RUN useradd -m appuser && chown -R appuser:appuser /app
USER appuser

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
