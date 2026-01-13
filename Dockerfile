# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build uber-jar
RUN mvn package -DskipTests -B -Dquarkus.package.jar.type=uber-jar

# Show what was built
RUN ls -la /app/target/

# Runtime stage  
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the uber-jar (named openonco-mcp-runner.jar)
COPY --from=build /app/target/openonco-mcp-runner.jar ./app.jar

EXPOSE 8080
ENV PORT=8080
CMD ["java", "-jar", "app.jar"]
