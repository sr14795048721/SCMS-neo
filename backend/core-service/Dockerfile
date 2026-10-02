FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY deploy/maven/settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY core-service/pom.xml core-service/pom.xml
COPY gateway-service/pom.xml gateway-service/pom.xml
COPY file-service/pom.xml file-service/pom.xml
RUN mvn -B -ntp -s /root/.m2/settings.xml -DskipTests -pl core-service -am dependency:go-offline
COPY . .
RUN mvn -B -ntp -s /root/.m2/settings.xml -DskipTests -pl core-service -am package

FROM eclipse-temurin:21-jre
ARG INSTALL_FFMPEG=true
ARG UBUNTU_APT_MIRROR=
RUN if [ "${INSTALL_FFMPEG}" = "true" ]; then \
        if [ -n "${UBUNTU_APT_MIRROR}" ] && [ -f /etc/apt/sources.list.d/ubuntu.sources ]; then \
            sed -i "s|http://archive.ubuntu.com/ubuntu|https://${UBUNTU_APT_MIRROR}/ubuntu|g" /etc/apt/sources.list.d/ubuntu.sources; \
            sed -i "s|http://security.ubuntu.com/ubuntu|https://${UBUNTU_APT_MIRROR}/ubuntu|g" /etc/apt/sources.list.d/ubuntu.sources; \
        fi; \
        apt-get update; \
        apt-get install -y --no-install-recommends ffmpeg; \
        rm -rf /var/lib/apt/lists/*; \
    fi
WORKDIR /app
COPY --from=build /workspace/core-service/target/core-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-Xms256m", "-Xmx512m", "-jar", "/app/app.jar"]
