# 🎮 Game Hub

Game Hub — это игровая платформа с модульной монорепозиторной архитектурой, объединяющая Spring Boot бэкенд и фронтенд-игры на TypeScript/Vite с изоляцией общих ресурсов через единую библиотеку `shared-frontend`.

---

## 🚀 Возможности

- 🔐 **Сессионный контроль доступа:** Защита игровых маршрутов через Spring Security и `HttpSession`
- 🎮 **Главная страница:** Выбор доступных игр и навигация
- ♟ **[Chess Game](./chess-backend/README.md):** Полноценный шахматный модуль
- ⚫ **[Checkers Game](./checkers-backend/README.md):** Русские шашки: обязательное взятие, летающие дамки, игра с ботом и по приглашению
- 🃏 **Blackjack:** Блэкджек (в планах)
- 📜 **История партий:** Сайдбар статистики и логов
- 🏆 **Рейтинг игроков:** Таблица лидеров
- 🔄 **Real-time синхронизация:** Состояние партий через WebSocket
- ⚡ **Virtual HTML:** Генерация HTML и единая обвязка Vite 8
- 🐳 **Docker:** Оптимизированный мультистейдж деплой

---

## 🔒 Безопасность и Доступ (Spring Security)

Доступ к играм регулируется на уровне HTTP-сессий с помощью пользовательской логики авторизации в `SpringSecurityConfig`:

- **Публичные маршруты (`permitAll`):** Главная страница (`/`, `/index.html`).
- **Редирект при отказе:** Если пользователь пытается зайти на `/chess/**` или `/checkers/**` без разрешения или с истёкшей сессией, `AccessDeniedHandler` автоматически перенаправляет его на главную страницу `/`.

---

## 🏗 Архитектура

- `api-gateway`: Единая точка входа для всех запросов.
- **Backend-сервисы (`chess-backend`, `checkers-backend`):** Игровая логика на Spring Boot. Каждая игра слушает свой WebSocket (`/chess/ws`, `/checkers/ws`) по одинаковому протоколу.
- **Frontend-модули (`chess-frontend`, `checkers-frontend`):** Интерфейсы игр на TypeScript/Vite.
- `shared-frontend`: Общая библиотека для всех фронтенд-модулей (`file:../shared-frontend`). 

---

## 📦 Модули проекта

- `shared-frontend` — Базовый UI-каркас, сокеты, типы и плагины Vite.
- `chess-backend` — Игровая логика шахмат на Spring Boot + Spring Security.
- `chess-frontend` — Фронтенд шахмат (зависит от `shared-frontend`).
- `checkers-backend` — Игровая логика шашек на Java 21 + Lombok + Spring Boot (правила, бот Alpha-Beta, WebSocket).
- `checkers-frontend` — Фронтенд шашек (зависит от `shared-frontend`).
- `main-frontend` — Главная страница: логин и выбор игры.
- `api-gateway` — Единая точка входа.

---

## ⚙️ Локальный запуск

### Автоматическая сборка и запуск через Gradle

Gradle автоматически отслеживает изменения исходников через `fileTree`, устанавливает NPM-зависимости и собирает `shared-frontend` перед сборкой игр.

```bash
./gradlew bootRun
```

*Доступ по адресу:* `http://localhost:8080/`.

---

## 🐳 Docker Deployment

Мультистейдж Dockerfile собирает `shared-frontend`, транслирует бандлы в игры и запечатывает статичный дистрибутив в Spring Boot JAR.

**Сборка образа:**
```bash
docker build -t game-hub .
```

**Запуск контейнера:**
```bash
docker run --rm -p 8080:8080 -e PORT=8080 game-hub
```

*Приложение доступно по адресу:* `http://localhost:8080/` (игры: `/chess/`, `/checkers/`)

---

## 🧩 Текущий статус

| Модуль                 | Статус | Технологии                        |
|:-----------------------| :--- |:----------------------------------|
| **shared-frontend**    | ✅ Готов | TypeScript, Vite 8, EventBus      |
| **chess-frontend**     | ✅ Готов | TypeScript, Shared Infrastructure |
| **chess-backend**      | ✅ Готов | Kotlin, Spring Boot, WebSockets   |
| **checkers-frontend**  | ✅ Готов | TypeScript, Shared Infrastructure |
| **checkers-backend**   | ✅ Готов | Java 21, Lombok, Spring Boot, WebSockets |
| **blackjack-frontend** | ⏳ Планируется | TypeScript, Shared Infrastructure |
| **blackjack-backend**  | ⏳ Планируется | Go                                |


---

## 👨‍💻 Автор

**Andrej Krilovs**