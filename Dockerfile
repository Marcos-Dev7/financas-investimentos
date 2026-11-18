# Stage 1: Build da aplicação com Maven e JDK 25
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime enxuto e seguro com JRE 25 e controle de memória RAM (75% max do container)
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY --from=build /app/target/financas-investimentos-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
