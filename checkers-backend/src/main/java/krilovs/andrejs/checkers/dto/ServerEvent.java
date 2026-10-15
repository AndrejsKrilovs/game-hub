package krilovs.andrejs.checkers.dto;

/** События, которые сервер отправляет клиенту по WebSocket. */
public enum ServerEvent {
  STATE,
  MOVE,
  MOVES,
  ERROR,
  TOAST,
  GAME_ENDED,
  INVITE_CREATED,
  PLAYER_COLOR,
  WAITING_FOR_OPPONENT
}
