FROM maven:3.9.8-eclipse-temurin-21
WORKDIR /app
COPY . .
CMD ["sh", "-c", "mvn -q spring-boot:run -Dspring-boot.run.arguments=--server.port=${SERVER_PORT:-8080}"]
