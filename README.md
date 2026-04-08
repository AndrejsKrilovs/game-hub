# ♟ Chess Game (WebSocket + AI)

Real-time шахматное приложение с AI (Alpha-Beta pruning), написанное на Kotlin (backend) и TypeScript (frontend).

## 🚀 Features

- ♟ Полная логика шахмат
- 🤖 AI (Alpha-Beta pruning)
- 🔄 WebSocket real-time обновления
- 🎨 UI на TypeScript
- 📜 История ходов
- ♜ Рокировки, шах, мат, пат

---

## 🏗 Архитектура

frontend (TypeScript)
        ↓ WebSocket
backend (Kotlin, Spring)
        ↓
Game Engine (Board + Rules + AI)

---

## ⚙️ Запуск локально

### Backend

```bash
./gradlew bootRun
```

Приложение доступно по адресу:

http://localhost:8080

---

## 📡 WebSocket API

### START_GAME
```json
{ "type": "START_GAME", "color": "WHITE" }
```

### MOVE
```json
{ "type": "MOVE", "from": "e2", "to": "e4" }
```

### GET_MOVES
```json
{ "type": "GET_MOVES", "from": "e2" }
```

---

## 🧠 AI

Используется алгоритм:

- Alpha-Beta pruning
- Depth: 3

---

## 📦 Стек

- Kotlin (Spring Boot, WebSocket)
- TypeScript
- Docker

---

## 🚀 TODO

- Multiplayer (онлайн игра)
- ELO рейтинг
- Сохранение партий
- Анимации ходов

---

## 👨‍💻 Автор

Andrej Krilovs
