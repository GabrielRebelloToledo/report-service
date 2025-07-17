FROM eclipse-temurin:17-jdk AS build

# Crie o diretório da app
WORKDIR /app

# Copie tudo para dentro da imagem
COPY . .

# Empacote o projeto com Maven (já estamos no diretório correto)
RUN ./mvnw package -DskipTests

# Crie a imagem final
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copie o JAR empacotado da fase de build
COPY --from=build /app/target/*.jar app.jar

# Comando para rodar o app
ENTRYPOINT ["java", "-jar", "app.jar"]
