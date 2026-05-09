# 1. Build Stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Caching dependencies
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -DskipTests

# 2. Run Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl \
 && addgroup -S app \
 && adduser -S app -G app

COPY --from=build --chown=app:app /app/target/*.jar app.jar

USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
