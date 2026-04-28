# ♟ Chess Game (WebSocket + AI)

Real-time шахматное приложение с AI (Alpha-Beta pruning), написанное на **Kotlin (backend)** и **TypeScript (frontend)**.

---

## 🚀 Features

- ♟ Полная логика шахмат
- 🤖 AI (Alpha-Beta pruning)
- 🔄 WebSocket real-time обновления
- 🎨 UI на TypeScript
- 📜 История ходов
- ♜ Рокировки, шах, мат, пат
- ♟ Превращение пешки (promotion)

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

./gradlew bootRun

http://localhost:8080

---

## 📡 WebSocket API

Все сообщения имеют формат:

{
  "type": "EVENT_TYPE",
  "payload": {}
}

---

### START_GAME

{
  "type": "START_GAME",
  "payload": { "color": "WHITE" }
}

---

### END_GAME

{
  "type": "END_GAME"
}

---

### GET_MOVES

{
  "type": "GET_MOVES",
  "payload": { "from": "e2" }
}

Ответ:

{
  "type": "MOVES",
  "payload": {
    "moves": ["e3", "e4"]
  }
}

---

### MAKE_MOVE

{
  "type": "MAKE_MOVE",
  "payload": {
    "from": "e2",
    "to": "e4"
  }
}

Ответ:

{
  "type": "MOVE",
  "payload": {
    "from": "e2",
    "to": "e4",
    "piece": "Pawn",
    "color": "WHITE",
    "isCastling": false,
    "castlingType": null
  }
}

---

### PROMOTION

Сервер:

{
  "type": "PROMOTION",
  "payload": {
    "availablePieces": ["Queen", "Rook", "Bishop", "Knight"],
    "color": "WHITE"
  }
}

Клиент:

{
  "type": "PROMOTE",
  "payload": {
    "piece": "Queen"
  }
}

---

### STATE

{
  "type": "STATE",
  "payload": {
    "pieces": [...],
    "turn": "WHITE",
    "state": "NORMAL"
  }
}

---

### ERROR

{
  "type": "ERROR",
  "payload": {
    "message": "Некорректный ход"
  }
}

---

### GAME_ENDED

{
  "type": "GAME_ENDED",
  "payload": {
    "message": "Партия завершена досрочно"
  }
}

---

## 🧠 AI

- Alpha-Beta pruning
- Depth: 3–4

---

## 📦 Стек

- Kotlin (Spring Boot, WebSocket)
- TypeScript
- Docker

---

## 👨‍💻 Автор

Andrej Krilovs
