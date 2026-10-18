# FROM eclipse-temurin:21-jdk-jammy AS builder
# WORKDIR /workspace/app
# COPY pom.xml .
# COPY src src
# RUN ./mvnw clean package -DskipTests 2>/dev/null || mvn clean package -DskipTests

# FROM eclipse-temurin:21-jre-jammy
# WORKDIR /app
# COPY --from=builder /workspace/app/target/*.jar app.jar
# EXPOSE 8080
# ENTRYPOINT ["java", "-jar", "app.jar"]

# Stage 1: Build the Spring Boot application using Maven
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Stage 2: Run the application using Java
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
