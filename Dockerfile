# Etapa de build
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests



# Etapa final
FROM openjdk:17-jdk-alpine

WORKDIR /app

RUN apt-get update && apt-get install -y \
    libfreetype6 \
    libfontconfig1 \
    fonts-dejavu \
    ttf-dejavu

COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 10000

CMD ["java", "-jar", "/app/app.jar"]
