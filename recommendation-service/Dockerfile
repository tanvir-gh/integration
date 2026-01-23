FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy gradle wrapper first (rarely changes)
COPY gradlew .
COPY gradle gradle
RUN chmod +x gradlew

# Copy build files and download dependencies (cached unless dependencies change)
COPY build.gradle .
COPY settings.gradle .
RUN ./gradlew dependencies --no-daemon

# Copy source and build (changes frequently)
COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install tools and download New Relic agent (both rarely change)
RUN apk add --no-cache curl unzip && \
    mkdir -p /app/newrelic && \
    curl -L -o /tmp/newrelic-java.zip "https://download.newrelic.com/newrelic/java-agent/newrelic-agent/9.0.0/newrelic-java.zip" && \
    unzip -o /tmp/newrelic-java.zip -d /tmp && \
    cp /tmp/newrelic/newrelic.jar /app/newrelic/ && \
    rm -rf /tmp/newrelic-java.zip /tmp/newrelic && \
    apk del curl unzip

# Copy config (occasionally changes)
COPY newrelic/newrelic.yml /app/newrelic/

# Copy application jar (changes frequently)
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-javaagent:/app/newrelic/newrelic.jar", "-jar", "app.jar"]
