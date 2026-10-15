# 1. Сборка фронтенд-модулей (Node.js)
FROM node:24-alpine AS frontend-build
WORKDIR /app

# Копируем package.json всех модулей для кеширования слоев
COPY shared-frontend/package*.json ./shared-frontend/
COPY chess-frontend/package*.json ./chess-frontend/
COPY checkers-frontend/package*.json ./checkers-frontend/
COPY main-frontend/package*.json ./main-frontend/

# Сборка общего UI-каркаса
WORKDIR /app/shared-frontend
COPY shared-frontend/ ./
RUN npm install && npm run build

# Сборка шахмат
WORKDIR /app/chess-frontend
COPY chess-frontend/ ./
RUN npm install && npm run build

# Сборка шашек
WORKDIR /app/checkers-frontend
COPY checkers-frontend/ ./
RUN npm install && npm run build

# Сборка главного UI
WORKDIR /app/main-frontend
COPY main-frontend/ ./
RUN npm install && npm run build

# 2. Сборка Spring Boot (Java 21)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY src ./src
COPY chess-backend ./chess-backend

# Копируем собранные ассеты в статику перед сборкой JAR
COPY --from=frontend-build /app/main-frontend/dist ./src/main/resources/static
COPY --from=frontend-build /app/chess-frontend/dist ./src/main/resources/static/chess
COPY --from=frontend-build /app/checkers-frontend/dist ./src/main/resources/static/checkers

RUN chmod +x ./gradlew

# Исключаем вызов npm-тасок Gradle, так как ассеты уже собраны на этапе frontend-build
RUN ./gradlew bootJar -x syncFrontendStatic -x npmSharedBuild -x npmMainBuild -x npmChessBuild -x npmCheckersBuild --no-daemon

# 3. Минимальный образ для запуска
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/build/libs/*.jar app.jar

USER app

EXPOSE 10000
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]