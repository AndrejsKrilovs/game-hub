FROM node:24-alpine AS chess-frontend-build
WORKDIR /app/chess-frontend

COPY chess-frontend/package*.json ./
RUN npm install

COPY chess-frontend/ ./
RUN npm run build

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY src ./src
COPY chess-backend ./chess-backend

COPY --from=chess-frontend-build /app/chess-frontend/dist ./src/main/resources/static/chess

RUN chmod +x ./gradlew
RUN ./gradlew bootJar \
    -x npmInstall \
    -x npmBuild \
    -x copyFrontend \
    --no-daemon


FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/build/libs/*.jar app.jar

USER app

EXPOSE 10000
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]