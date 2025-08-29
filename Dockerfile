# Build stage
FROM maven:3.9.4-eclipse-temurin-17 as build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

# Runtime stage: install ffmpeg
FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y ffmpeg && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/video-streaming-ms-0.0.1-SNAPSHOT.jar /app/video.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/video.jar"]