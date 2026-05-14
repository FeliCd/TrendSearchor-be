# ==========================================
# Stage 1: Build the application
# ==========================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml
COPY pom.xml ./

# Download dependencies (this layer will be cached unless pom.xml changes)
RUN mvn dependency:go-offline

# Copy the actual source code
COPY src ./src

# Build the application, skipping tests to speed up the process
RUN mvn package -DskipTests

# ==========================================
# Stage 2: Create the final lightweight image
# ==========================================
FROM eclipse-temurin:17-jre-alpine

# Set the working directory
WORKDIR /app

# Copy the built jar file from the builder stage
# (Assuming the jar is generated in the target directory and ends with .jar)
COPY --from=builder /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Set environment variables for Java tuning (optional but recommended)
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Command to run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
