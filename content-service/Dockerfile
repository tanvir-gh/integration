FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl unzip

# Download New Relic agent
RUN mkdir -p /app/newrelic && \
    curl -L -o /tmp/newrelic-java.zip "https://download.newrelic.com/newrelic/java-agent/newrelic-agent/8.17.0/newrelic-java.zip" && \
    unzip -o /tmp/newrelic-java.zip -d /tmp && \
    cp /tmp/newrelic/newrelic.jar /app/newrelic/ && \
    rm -rf /tmp/newrelic-java.zip /tmp/newrelic

COPY newrelic/newrelic.yml /app/newrelic/
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-javaagent:/app/newrelic/newrelic.jar", "-jar", "app.jar"]
