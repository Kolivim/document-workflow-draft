# Stage 1: Build the application (Optional, for multi-stage build)
FROM eclipse-temurin:17-jre-alpine AS builder
WORKDIR /app
COPY . .
#RUN javac HelloWorld.java # For a simple .java file
# Or, for a Maven project:
# COPY pom.xml ./
# RUN mvn dependency:go-offline
# COPY src ./src
RUN mvn package

# Stage 2: Run the application (Production image)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy the JAR file from the builder stage if using multi-stage
# COPY --from=builder /app/target/myapp.jar /app/myapp.jar

# Or, if building the JAR locally before running docker build:
COPY target/itg-test-1.0-SNAPSHOT.jar /app/itg-test-1.0-SNAPSHOT.jar

EXPOSE 8080

CMD ["java", "-jar", "itg-test-1.0-SNAPSHOT.jar"]