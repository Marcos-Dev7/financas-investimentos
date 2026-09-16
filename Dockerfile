FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY target/financas-investimentos-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
CMD [ "java" ,"-jar" ,"app.jar" ]