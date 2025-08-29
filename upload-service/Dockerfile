# Build stage
FROM maven:3.9.4-eclipse-temurin-17 as build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
# create directory and copy app jar
WORKDIR /app
COPY --from=build /app/target/upload-service-0.0.1-SNAPSHOT.jar /app/upload.jar
# copy entrypoint helper
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# container will see secrets at /run/secrets/<name> when launched via docker stack deploy or compose when supported
EXPOSE 8081
ENTRYPOINT ["/app/entrypoint.sh"]