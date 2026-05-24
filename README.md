# 🎮 Game Hub

Game Hub — это игровая платформа с авторизацией, выбором игр и отдельными игровыми сервисами.

Проект развивается как multi-module / multi-service приложение. Каждая игра находится в отдельном модуле, а доступ к играм и общим сервисам проходит через единый входной слой.

---

## 🚀 Возможности

- 🔐 Регистрация и авторизация пользователей
- 🎮 Главная страница с выбором игр
- ♟ [Chess Game](./chess-backend/README.md)
- ⚫ Checkers
- 🃏 Blackjack
- 📜 История партий
- 🏆 Рейтинг и статистика игроков
- 🔄 Real-time обновления через WebSocket
- 🐳 Docker deployment

---

## 🏗 Архитектура

```text
browser / frontend
        ↓
api-gateway
        ↓
 ┌─────────────────────┐
 │ auth-service        │
 │ chess-backend       │
 │ checkers-backend    │
 │ blackjack-backend   │
 └─────────────────────┘
```

---

## 📦 Модули проекта

```text
game-hub/
  api-gateway/
  chess-backend/
  chess-frontend/
  checkers-backend/
  checkers-frontend/
  blackjack-backend/
  blackjack-frontend/
```

### `api-gateway`

Единая точка входа в приложение.

Планируемые маршруты:

```text
/              → главная страница
/auth/**       → регистрация / авторизация
/chess/**      → шахматы
/checkers/**   → шашки
/blackjack/**  → blackjack
```

### `chess-backend`

Backend шахматной игры.

Подробнее: [Chess Game README](./chess-backend/README.md)

### `chess-frontend`

Frontend шахматной игры на TypeScript.

### `checkers-backend`

Планируемый backend для шашек.

### `checkers-frontend`

Планируемый frontend для шашек.

### `blackjack-backend`

Планируемый backend для blackjack. Возможная реализация — Node.js.

### `blackjack-frontend`

Планируемый frontend для blackjack.

---

## ⚙️ Локальный запуск

### Через API Gateway

Терминал 1:

```bash
./gradlew :chess-backend:bootRun
```

Терминал 2:

```bash
./gradlew :api-gateway:bootRun
```

Открыть:

```text
http://localhost:8080/chess/
```

### Шахматы напрямую без gateway

```bash
./gradlew :chess-backend:bootRun
```

Открыть:

```text
http://localhost:8081/chess/
```

---

## 🐳 Docker

Сборка image:

```bash
docker build -t game-hub .
```

Запуск:

```bash
docker run --rm -p 8080:8080 -e PORT=8080 game-hub
```

---

## 🌍 Deployment

Проект может деплоиться на:

- Render
- Docker Hub
- VPS
- Oracle Cloud Free Tier
- Railway
- Koyeb

Для production рекомендуется запуск через `api-gateway`.

---

## 🧩 Текущий статус

| Модуль | Статус |
|---|---|
| API Gateway | Планируется / добавляется |
| Auth | Планируется |
| Chess backend | В разработке |
| Chess frontend | В разработке |
| Checkers | Планируется |
| Blackjack | Планируется |

---

## 🛠 Стек

### Backend

- Kotlin
- Java
- Spring Boot
- Spring WebSocket
- Spring Cloud Gateway
- Gradle

### Frontend

- TypeScript
- Vite
- HTML/CSS

### Deployment

- Docker
- Render

---

## 👨‍💻 Автор

Andrej Krilovs
