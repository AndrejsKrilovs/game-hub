package krilovs.andrejs.checkers.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import krilovs.andrejs.checkers.application.bot.CheckersBot;
import krilovs.andrejs.checkers.domain.model.Color;
import krilovs.andrejs.checkers.domain.model.Move;
import krilovs.andrejs.checkers.dto.AvailableMovesResult;
import krilovs.andrejs.checkers.dto.ClientCommand;
import krilovs.andrejs.checkers.dto.MoveResult;
import krilovs.andrejs.checkers.dto.ServerEvent;
import krilovs.andrejs.checkers.dto.StatePayload;
import krilovs.andrejs.checkers.dto.WsEvent;
import krilovs.andrejs.checkers.utils.BoardUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckersWebsocketHandler extends TextWebSocketHandler {

  private static final String GAME_SESSION_ID_PARAM = "gameSessionId";
  private static final int MAX_PLAYERS = 2;

  private final ObjectMapper mapper;
  private final ObjectProvider<CheckersGameService> gameServiceProvider;
  private final CheckersBot bot;

  @Value("${app.websocket.empty-game-ttl-seconds:60}")
  private final long emptyGameTtlSeconds;

  private final Map<String, CheckersGameService> games = new ConcurrentHashMap<>();
  private final Map<String, String> sessionToGame = new ConcurrentHashMap<>();
  private final Map<String, Set<WebSocketSession>> gameSessions = new ConcurrentHashMap<>();
  private final Map<String, Color> botColors = new ConcurrentHashMap<>();
  private final Set<String> invitedGames = ConcurrentHashMap.newKeySet();
  private final Map<String, Color> inviteHostColors = new ConcurrentHashMap<>();
  private final Map<String, Color> playerColors = new ConcurrentHashMap<>();
  private final Map<String, ScheduledFuture<?>> cleanupTasks = new ConcurrentHashMap<>();
  private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(
    Thread.ofPlatform().name("checkers-game-cleanup").daemon().factory()
  );

  @PreDestroy
  public void shutdown() {
    cleanupExecutor.shutdownNow();
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    var requestedGameSessionId = queryParam(session);
    var gameSessionId = requestedGameSessionId != null ? requestedGameSessionId : session.getId();
    var game = resolveGame(gameSessionId, requestedGameSessionId != null);

    if (game == null) {
      reject(session, "Игра не найдена");
      return;
    }

    if (requestedGameSessionId != null && openSessionsCount(gameSessionId) >= MAX_PLAYERS) {
      reject(session, "В игре уже два игрока");
      return;
    }

    registerSession(gameSessionId, session);
    if (requestedGameSessionId != null) {
      startInviteGameIfReady(gameSessionId, game);
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    unregisterSession(session);
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    JsonNode data;
    try {
      data = mapper.readTree(message.getPayload());
    }
    catch (JsonProcessingException e) {
      sendError(session, "Некорректный JSON");
      return;
    }

    var rawType = data.path("type").asText(null);
    var command = ClientCommand.parse(rawType);
    if (command.isEmpty()) {
      sendError(session, "Неизвестный тип сообщения: " + rawType);
      return;
    }

    var payload = data.get("payload");
    switch (command.get()) {
      case GET_MOVES -> withPayload(session, payload, p -> handleGetMoves(session, p));
      case MAKE_MOVE -> withPayload(session, payload, p -> handleMove(session, p));
      case START_GAME -> withPayload(session, payload, p -> handleStartGame(session, p));
      case INVITE -> withPayload(session, payload, p -> handleInvite(session, p));
      case END_GAME -> handleEndGame(session);
      case PING -> {
      }
    }
  }

  private CheckersGameService resolveGame(String gameSessionId, boolean existingGameOnly) {
    return existingGameOnly
      ? games.get(gameSessionId)
      : games.computeIfAbsent(gameSessionId, id -> gameServiceProvider.getObject());
  }

  private int openSessionsCount(String gameSessionId) {
    return (int) gameSessions.getOrDefault(gameSessionId, Set.of()).stream()
      .filter(WebSocketSession::isOpen)
      .count();
  }

  private void registerSession(String gameSessionId, WebSocketSession session) {
    cancelCleanup(gameSessionId);
    sessionToGame.put(session.getId(), gameSessionId);
    gameSessions.computeIfAbsent(gameSessionId, id -> ConcurrentHashMap.newKeySet()).add(session);
  }

  private void unregisterSession(WebSocketSession session) {
    playerColors.remove(session.getId());

    var gameSessionId = sessionToGame.remove(session.getId());
    var sessions = gameSessionId == null ? null : gameSessions.get(gameSessionId);
    if (sessions == null) {
      return;
    }

    sessions.remove(session);

    if (sessions.isEmpty()) {
      gameSessions.remove(gameSessionId);
      if (invitedGames.contains(gameSessionId)) {
        scheduleEmptyGameCleanup(gameSessionId);
      }
      else {
        removeGame(gameSessionId);
      }
    }
    else if (invitedGames.contains(gameSessionId)) {
      sendToGame(gameSessionId, ServerEvent.GAME_ENDED, Map.of("message", "Соперник покинул игру"));
      removeGame(gameSessionId);
    }
  }

  private void scheduleEmptyGameCleanup(String gameSessionId) {
    var task = cleanupExecutor.schedule(() -> {
      cleanupTasks.remove(gameSessionId);
      if (openSessionsCount(gameSessionId) == 0) {
        removeGame(gameSessionId);
      }
    }, emptyGameTtlSeconds, TimeUnit.SECONDS);

    Optional.ofNullable(cleanupTasks.put(gameSessionId, task)).ifPresent(previous -> previous.cancel(false));
  }

  private void cancelCleanup(String gameSessionId) {
    Optional.ofNullable(cleanupTasks.remove(gameSessionId)).ifPresent(task -> task.cancel(false));
  }

  /** Партия против бота: игрок выбирает цвет, бот играет противоположным. */
  private void handleStartGame(WebSocketSession session, JsonNode data) {
    var game = game(session);
    var gameSessionId = sessionToGame.get(session.getId());
    var playerColor = readColor(session, data);
    if (game == null || gameSessionId == null || playerColor == null) {
      return;
    }

    var botColor = playerColor.opposite();
    synchronized (game) {
      game.reset();
    }
    botColors.put(gameSessionId, botColor);
    playerColors.put(session.getId(), playerColor);

    sendStateToGame(gameSessionId, game);
    handleBotMove(gameSessionId, game, botColor);
  }

  private void handleInvite(WebSocketSession session, JsonNode data) {
    var oldGameSessionId = sessionToGame.get(session.getId());
    var game = game(session);
    var playerColor = readColor(session, data);
    if (oldGameSessionId == null || game == null || playerColor == null) {
      return;
    }

    var newGameSessionId = UUID.randomUUID().toString();

    games.remove(oldGameSessionId);
    games.put(newGameSessionId, game);
    botColors.remove(oldGameSessionId);
    invitedGames.remove(oldGameSessionId);
    invitedGames.add(newGameSessionId);
    inviteHostColors.put(newGameSessionId, playerColor);
    playerColors.put(session.getId(), playerColor);
    moveSessionToGame(session, oldGameSessionId, newGameSessionId);

    sendEvent(session, ServerEvent.INVITE_CREATED, Map.of(
      "gameSessionId", newGameSessionId,
      "inviteUrl", buildInviteUrl(session, newGameSessionId),
      "color", playerColor,
      "message", "Ссылка приглашения создана. Ожидаем второго игрока."
    ));
    sendEvent(session, ServerEvent.PLAYER_COLOR, Map.of("color", playerColor));
    sendEvent(session, ServerEvent.WAITING_FOR_OPPONENT, Map.of("message", "Ожидаем подключения второго игрока..."));
  }

  private void handleEndGame(WebSocketSession session) {
    var gameSessionId = sessionToGame.get(session.getId());
    if (gameSessionId == null) {
      return;
    }

    sendToGame(gameSessionId, ServerEvent.GAME_ENDED, Map.of("message", "Партия завершена досрочно!"));
    removeGame(gameSessionId);
  }

  private void handleGetMoves(WebSocketSession session, JsonNode data) {
    var game = game(session);
    var from = readSquare(session, data, "from");
    if (game == null || from == null || !ownsPiece(session, game, from)) {
      return;
    }

    AvailableMovesResult result;
    synchronized (game) {
      result = game.generateMovesForSquare(from);
    }

    switch (result) {
      case AvailableMovesResult.Success(var moves) ->
        sendEvent(session, ServerEvent.MOVES, Map.of("moves", moves.stream().map(Move::toDto).toList()));
      case AvailableMovesResult.Error(var message) -> sendError(session, message);
    }
  }

  private void handleMove(WebSocketSession session, JsonNode data) {
    var game = game(session);
    var gameSessionId = sessionToGame.get(session.getId());
    var from = readSquare(session, data, "from");
    var to = readSquare(session, data, "to");
    if (game == null || gameSessionId == null || from == null || to == null || !ownsPiece(session, game, from)) {
      return;
    }

    MoveResult result;
    synchronized (game) {
      result = game.makeMove(from, to);
    }

    switch (result) {
      case MoveResult.Success(var move) -> {
        sendMoveAndState(gameSessionId, move, game);

        var botColor = botColors.get(gameSessionId);
        if (botColor != null) {
          handleBotMove(gameSessionId, game, botColor);
        }
      }
      case MoveResult.Error(var message) -> sendError(session, message);
    }
  }

  private void handleBotMove(String gameSessionId, CheckersGameService game, Color botColor) {
    synchronized (game) {
      if (game.getWinner() != null || game.getCurrentTurn() != botColor) {
        return;
      }
    }

    // Поиск идёт на копии доски, поэтому игра во время него не блокируется
    var turn = bot.findBestTurn(game);
    if (turn == null) {
      return;
    }

    // Серия взятий применяется по отрезкам, как если бы её делал игрок
    var from = turn.from();
    for (int to : turn.path()) {
      MoveResult result;
      synchronized (game) {
        result = game.makeMove(from, to);
      }

      switch (result) {
        case MoveResult.Success(var move) -> sendMoveAndState(gameSessionId, move, game);
        case MoveResult.Error(var message) -> {
          log.warn("Бот сделал недопустимый ход {} -> {}: {}", from, to, message);
          sendToGame(gameSessionId, ServerEvent.ERROR, Map.of("message", message));
          return;
        }
      }
      from = to;
    }
  }

  private boolean   ownsPiece(WebSocketSession session, CheckersGameService game, int square) {
    var playerColor = playerColors.get(session.getId());
    if (playerColor == null) {
      return true;
    }

    var piece = game.getPiece(square);
    if (piece == null || piece.color() != playerColor) {
      sendError(session, "Это не ваша фигура");
      return false;
    }
    return true;
  }

  private void moveSessionToGame(WebSocketSession session, String oldGameSessionId, String newGameSessionId) {
    var oldSessions = gameSessions.get(oldGameSessionId);
    if (oldSessions != null) {
      oldSessions.remove(session);
      if (oldSessions.isEmpty()) {
        gameSessions.remove(oldGameSessionId);
      }
    }

    sessionToGame.put(session.getId(), newGameSessionId);
    gameSessions.computeIfAbsent(newGameSessionId, id -> ConcurrentHashMap.newKeySet()).add(session);
  }

  private void removeGame(String gameSessionId) {
    games.remove(gameSessionId);
    botColors.remove(gameSessionId);
    invitedGames.remove(gameSessionId);
    inviteHostColors.remove(gameSessionId);
    cancelCleanup(gameSessionId);

    var sessions = gameSessions.remove(gameSessionId);
    if (sessions == null) {
      return;
    }

    for (var session : sessions) {
      sessionToGame.remove(session.getId());
      playerColors.remove(session.getId());
      closeQuietly(session, CloseStatus.NORMAL);
    }
  }

  private CheckersGameService game(WebSocketSession session) {
    var gameSessionId = sessionToGame.get(session.getId());
    return gameSessionId == null ? null : games.get(gameSessionId);
  }

  private StatePayload buildStatePayload(CheckersGameService game) {
    synchronized (game) {
      return new StatePayload(game.getPiecesDto(), game.getCurrentTurn(), game.getGameState(), game.getWinner());
    }
  }

  private void sendMoveAndState(String gameSessionId, Move move, CheckersGameService game) {
    sendToGame(gameSessionId, ServerEvent.MOVE, Map.of("move", move.toDto()));
    sendStateToGame(gameSessionId, game);
  }

  private void sendStateToGame(String gameSessionId, CheckersGameService game) {
    sendToGame(gameSessionId, ServerEvent.STATE, buildStatePayload(game));
  }

  private void sendToGame(String gameSessionId, ServerEvent type, Object payload) {
    gameSessions.getOrDefault(gameSessionId, Set.of()).stream()
      .filter(WebSocketSession::isOpen)
      .forEach(session -> sendEvent(session, type, payload));
  }

  private void sendError(WebSocketSession session, String message) {
    sendEvent(session, ServerEvent.ERROR, Map.of("message", message));
  }

  private void sendEvent(WebSocketSession session, ServerEvent type, Object payload) {
    if (!session.isOpen()) {
      return;
    }

    try {
      var json = mapper.writeValueAsString(new WsEvent(type, payload));
      // WebSocketSession не потокобезопасна: ответы идут из потока сокета и из потока бота
      synchronized (session) {
        session.sendMessage(new TextMessage(json));
      }
    }
    catch (IOException | IllegalStateException e) {
      log.debug("Не удалось отправить {} в сессию {}: {}", type, session.getId(), e.getMessage());
    }
  }

  private void reject(WebSocketSession session, String message) {
    sendError(session, message);
    closeQuietly(session, CloseStatus.NOT_ACCEPTABLE);
  }

  private void closeQuietly(WebSocketSession session, CloseStatus status) {
    try {
      session.close(status);
    }
    catch (IOException e) {
      log.debug("Не удалось закрыть сессию {}: {}", session.getId(), e.getMessage());
    }
  }

  private void withPayload(WebSocketSession session, JsonNode payload, Consumer<JsonNode> handler) {
    if (payload == null || payload.isNull()) {
      sendError(session, "Payload отсутствует");
      return;
    }
    handler.accept(payload);
  }

  private Integer readSquare(WebSocketSession session, JsonNode data, String field) {
    var value = data.path(field).asText(null);
    if (value == null || value.isBlank()) {
      sendError(session, "Не указана клетка: " + field);
      return null;
    }

    try {
      return BoardUtils.toSquare(value);
    }
    catch (IllegalArgumentException e) {
      sendError(session, "Некорректная клетка: " + value);
      return null;
    }
  }

  private Color readColor(WebSocketSession session, JsonNode data) {
    var value = data.path("color").asText(null);
    if (value == null || value.isBlank()) {
      sendError(session, "Не указан цвет");
      return null;
    }

    try {
      return Color.valueOf(value);
    }
    catch (IllegalArgumentException e) {
      sendError(session, "Некорректный цвет: " + value);
      return null;
    }
  }

  private String queryParam(WebSocketSession session) {
    var query = session.getUri() == null ? null : session.getUri().getRawQuery();
    if (query == null) {
      return null;
    }

    for (var rawParam : query.split("&")) {
      var parts = rawParam.split("=", 2);
      if (parts.length == 2 && URLDecoder.decode(parts[0], StandardCharsets.UTF_8).equals(CheckersWebsocketHandler.GAME_SESSION_ID_PARAM)) {
        return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
      }
    }
    return null;
  }

  private String buildInviteUrl(WebSocketSession session, String gameSessionId) {
    var origin = session.getHandshakeHeaders().getOrigin();
    var base = origin == null || origin.isBlank() ? "" : origin.replaceAll("/+$", "");

    return base + "/checkers?gameSessionId=" + gameSessionId;
  }

  private void startInviteGameIfReady(String gameSessionId, CheckersGameService game) {
    if (!invitedGames.contains(gameSessionId)) {
      sendStateToGame(gameSessionId, game);
      return;
    }

    if (openSessionsCount(gameSessionId) < MAX_PLAYERS) {
      return;
    }

    synchronized (game) {
      game.reset();
    }

    var hostColor = inviteHostColors.get(gameSessionId);
    for (var session : List.copyOf(gameSessions.getOrDefault(gameSessionId, Set.of()))) {
      if (!session.isOpen()) {
        continue;
      }

      var color = playerColors.get(session.getId());
      if (color == null && hostColor != null) {
        color = hostColor.opposite();
        playerColors.put(session.getId(), color);
      }
      if (color != null) {
        sendEvent(session, ServerEvent.PLAYER_COLOR, Map.of("color", color));
      }
    }

    sendToGame(gameSessionId, ServerEvent.TOAST, Map.of(
      "message", "Второй игрок подключился. Партия началась!",
      "type", "success"
    ));
    sendStateToGame(gameSessionId, game);
  }
}
