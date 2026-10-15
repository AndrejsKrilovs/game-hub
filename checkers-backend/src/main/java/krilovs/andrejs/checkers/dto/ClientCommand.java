package krilovs.andrejs.checkers.dto;

import java.util.Arrays;
import java.util.Optional;

/** Команды, которые клиент отправляет по WebSocket. */
public enum ClientCommand {
  GET_MOVES,
  MAKE_MOVE,
  START_GAME,
  INVITE,
  END_GAME,
  PING;

  public static Optional<ClientCommand> parse(String value) {
    return Arrays.stream(values())
      .filter(command -> command.name().equals(value))
      .findFirst();
  }
}
