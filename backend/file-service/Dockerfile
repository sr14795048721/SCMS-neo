FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY core-service/pom.xml core-service/pom.xml
COPY gateway-service/pom.xml gateway-service/pom.xml
COPY file-service/pom.xml file-service/pom.xml
RUN mvn -q -DskipTests -pl file-service -am dependency:go-offline
COPY . .
RUN mvn -q -DskipTests -pl file-service -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/file-service/target/file-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-Xms128m", "-Xmx384m", "-jar", "/app/app.jar"]
