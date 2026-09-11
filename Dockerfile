# 1. Сборка фронтенд-модулей (Node.js)
FROM node:24-alpine AS frontend-build
WORKDIR /app

# Копируем package.json обоиx модулей
COPY shared-frontend/package*.json ./shared-frontend/
COPY chess-frontend/package*.json ./chess-frontend/

# Сборка общего UI-каркаса
WORKDIR /app/shared-frontend
RUN npm install
COPY shared-frontend/ ./
RUN npm run build

# Сборка шахмат (видит ../shared-frontend/dist)
WORKDIR /app/chess-frontend
RUN npm install
COPY chess-frontend/ ./
RUN npm run build

# 2. Сборка Spring Boot (Java 21)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY src ./src
COPY chess-backend ./chess-backend

# Подкладываем скомпилированный фронтенд в static шахмат
COPY --from=frontend-build /app/chess-frontend/dist ./src/main/resources/static/chess

RUN chmod +x ./gradlew
# Отключаем Gradle-таску по её НОВОМУ имени: copyChessFrontend
RUN ./gradlew bootJar -x copyChessFrontend --no-daemon

# 3. Минимальный образ для запуска
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/build/libs/*.jar app.jar

USER app

EXPOSE 10000
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]