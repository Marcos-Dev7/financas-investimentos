# Stage 1: Build da aplicação com Maven e JDK 25
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime enxuto e seguro com JRE 25
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY --from=build /app/target/financas-investimentos-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
