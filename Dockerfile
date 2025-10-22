# ---- Stage 1: Build the app ----
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -DskipTests

# ---- Stage 2: Run the app ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the packaged JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Render provides a dynamic PORT environment variable
ENV PORT=8080
EXPOSE 8080

# Run the application with dynamic port support
ENTRYPOINT ["java", "-jar", "app.jar"]
