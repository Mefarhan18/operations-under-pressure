FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace/app
COPY pom.xml .
COPY src src
RUN ./mvnw clean package -DskipTests 2>/dev/null || mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /workspace/app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
