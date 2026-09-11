# 1. Сборка всех фронтенд-модулей
FROM node:24-alpine AS frontend-build
WORKDIR /app

# Копируем манифесты для эффективного кэширования слоев npm
COPY shared-frontend/package*.json ./shared-frontend/
COPY chess-frontend/package*.json ./chess-frontend/

# Сборка shared-frontend
WORKDIR /app/shared-frontend
RUN npm install
COPY shared-frontend/ ./
RUN npm run build

# Сборка chess-frontend (зависит от собранного shared-frontend)
WORKDIR /app/chess-frontend
RUN npm install
COPY chess-frontend/ ./
RUN npm run build

# 2. Сборка Spring Boot бэкенда
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY src ./src
COPY chess-backend ./chess-backend

# Копируем готовый dist шахмат напрямую в static-ресурсы Spring
COPY --from=frontend-build /app/chess-frontend/dist ./src/main/resources/static/chess

RUN chmod +x ./gradlew
# Отключаем таску копирования фронтенда в Gradle, так как статика уже подложена
RUN ./gradlew bootJar \
    -x copyChessFrontend \
    --no-daemon

# 3. Минималистичный образ для продакшена
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/build/libs/*.jar app.jar

USER app

EXPOSE 10000
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]