FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copia só o pom.xml primeiro pra aproveitar cache do Docker
# (só baixa as dependências de novo se o pom.xml mudar)
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Estágio final: só o necessário pra RODAR o app ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
