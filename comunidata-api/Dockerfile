# Stage 1: Build the application with Maven
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the entire source code and build the project
COPY src ./src
RUN mvn clean package -DskipTests

# Extract Spring Boot layers
RUN java -Djarmode=layertools -jar target/*.jar extract

# Stage 2: Build a lightweight runtime image
FROM openjdk:21-slim
WORKDIR /workspace

# Install graphics libraries required for JFreeChart
RUN apt-get update && apt-get install -y \
    libfreetype6 \
    libfontconfig1 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libxrandr2 \
    libasound2 \
    && rm -rf /var/lib/apt/lists/*

# Copy Spring Boot layers from the builder stage
COPY --from=builder /workspace/dependencies/ ./
COPY --from=builder /workspace/spring-boot-loader/ ./
COPY --from=builder /workspace/snapshot-dependencies/ ./
COPY --from=builder /workspace/application/ ./

# Expose the port that Spring Boot will run on
EXPOSE 8080

# Start the application using JarLauncher and specifying the config file location
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]


