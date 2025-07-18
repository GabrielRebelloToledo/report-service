FROM openjdk:17-jdk-alpine AS build
RUN mkdir /app

# Crie o diretório da app
WORKDIR /app

# Copie tudo para dentro da imagem
COPY target/*.jar /app/app.jar

EXPOSE 10000

RUN mvn clean package -DskipTests


CMD ["java", ".jar", "/app/app.jar"]