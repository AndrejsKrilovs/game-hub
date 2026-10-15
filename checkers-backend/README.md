# ⚫ Checkers Game (WebSocket + AI)

Real-time приложение для игры в русские шашки с AI (Alpha-Beta pruning), написанное на **Java 21 + Lombok + Spring Boot (backend)** и **TypeScript (frontend)**.

---

## 🚀 Features

- ⚫ Правила русских шашек (доска 8×8)
- ⚔️ Обязательное взятие и серии взятий
- 👑 Летающие дамки, превращение в дамку (в том числе в серии взятий)
- 🤖 AI (Alpha-Beta pruning)
- 👥 Игра по ссылке-приглашению
- 🔄 WebSocket real-time обновления
- 🎨 UI на TypeScript
- 📜 История ходов

---

## 🏗 Архитектура

frontend (TypeScript)
        ↓ WebSocket (/checkers/ws)
backend (Java, Spring)
        ↓
Game Engine (Board + MoveGenerator + AI)

Структура модуля (`krilovs.andrejs.checkers`):

- `domain` — `Board`, `Piece`, `Move`, `MoveGenerator` (правила)
- `application` — `CheckersGameService` (состояние партии), `CheckersWebsocketHandler` (сессии, приглашения, события)
- `application.bot` — `CheckersBot`, `CheckersSearch` (Alpha-Beta), `TurnGenerator`, `BoardEvaluator`
- `dto` — команды, события и payload'ы протокола

---

## ⚙️ Запуск локально

### Backend

./gradlew bootRun

http://localhost:8080/checkers/

---

## 📏 Правила

- Простая шашка ходит вперёд по диагонали на одну клетку, бьёт в любую сторону
- Дамка ходит и бьёт на любое расстояние по диагонали
- Взятие обязательно. В серии взятий бить нужно той же фигурой до конца
- Побитые в серии фигуры снимаются после завершения хода и не могут быть побиты дважды
- Шашка, дошедшая до последней горизонтали (в том числе при взятии), становится дамкой и продолжает бить как дамка
- Проигрывает тот, у кого нет фигур или ходов
- Ничьей в текущей версии нет

---

## 📡 WebSocket API

Все сообщения имеют формат:

{
  "type": "EVENT_TYPE",
  "payload": {}
}

---

### START_GAME

Партия против бота. Бот играет противоположным цветом.

{
  "type": "START_GAME",
  "payload": { "color": "WHITE" }
}

---

### INVITE

Создаёт ссылку-приглашение для игры с другим человеком.

{
  "type": "INVITE",
  "payload": { "color": "WHITE" }
}

Ответ:

{
  "type": "INVITE_CREATED",
  "payload": {
    "gameSessionId": "...",
    "inviteUrl": "http://localhost:8080/checkers?gameSessionId=...",
    "color": "WHITE",
    "message": "..."
  }
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
  "payload": { "from": "c3" }
}

Ответ:

{
  "type": "MOVES",
  "payload": {
    "moves": [
      { "from": "c3", "to": "d4", "piece": "Man", "color": "WHITE", "promoted": false, "continues": false }
    ]
  }
}

---

### MAKE_MOVE

Каждый прыжок в серии взятий отправляется отдельным ходом.

{
  "type": "MAKE_MOVE",
  "payload": {
    "from": "c3",
    "to": "e5"
  }
}

Ответ:

{
  "type": "MOVE",
  "payload": {
    "move": {
      "from": "c3",
      "to": "e5",
      "piece": "Man",
      "color": "WHITE",
      "captured": "d4",
      "promoted": false,
      "continues": true
    }
  }
}

`continues: true` — серия взятий не закончена, ход остаётся у той же фигуры.

---

### STATE

{
  "type": "STATE",
  "payload": {
    "pieces": [
      { "type": "Man", "color": "WHITE", "coordinates": "a1", "captured": false }
    ],
    "turn": "WHITE",
    "state": "NORMAL"
  }
}

`state` — `NORMAL` или `WIN` (тогда добавляется `winner`). `captured: true` — фигура побита в текущей серии, ход ещё не завершён.

---

### PLAYER_COLOR / WAITING_FOR_OPPONENT / TOAST

Служебные события для игры по приглашению: назначенный цвет, ожидание соперника и уведомления.

---

### ERROR

{
  "type": "ERROR",
  "payload": {
    "message": "Обязательно нужно бить"
  }
}

---

### GAME_ENDED

{
  "type": "GAME_ENDED",
  "payload": {
    "message": "Партия завершена досрочно!"
  }
}

---

## 🧠 AI

- Negamax с Alpha-Beta pruning
- Итеративное углубление, лимит по времени
- Упорядочивание ходов (взятия первыми), продление поиска при обязательных взятиях
- Оценка: материал, продвижение шашек, центр, задняя линия

Настройки (`application.yml`):

checkers:
  bot:
    max-depth: 8
    time-limit-ms: 1200

---

## 📦 Стек

- Java 21, Lombok
- Spring Boot, WebSocket
- TypeScript
- Docker

---

## 👨‍💻 Автор

Andrej Krilovs
