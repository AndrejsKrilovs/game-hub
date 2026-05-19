FROM node:24-alpine AS frontend-build
WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm install

COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:21-jdk-alpine AS backend-build
WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY backend ./backend

COPY --from=frontend-build /app/frontend/dist ./backend/src/main/resources/static

RUN chmod +x ./gradlew
RUN ./gradlew :backend:bootJar \
  -x npmInstall \
  -x npmBuild \
  -x copyFrontend \
  --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=backend-build /app/backend/build/libs/*.jar app.jar
USER app

EXPOSE 10000
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]