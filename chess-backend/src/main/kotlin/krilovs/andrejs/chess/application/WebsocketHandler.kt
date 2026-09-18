package krilovs.andrejs.chess.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import krilovs.andrejs.chess.application.bot.ChessBot
import krilovs.andrejs.chess.domain.model.Color
import krilovs.andrejs.chess.dto.AvailableMovesResult
import krilovs.andrejs.chess.dto.MoveResult
import krilovs.andrejs.chess.dto.PromotionResult
import krilovs.andrejs.chess.utils.BoardUtils
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class WebsocketHandler(
  private val mapper: ObjectMapper,
  private val gameServiceProvider: ObjectProvider<GameService>,
  private val bot: ChessBot
) : TextWebSocketHandler() {

  private companion object {
    private const val GAME_SESSION_ID_PARAM = "gameSessionId"
    private const val GET_MOVES = "GET_MOVES"
    private const val PROMOTE = "PROMOTE"
    private const val MAKE_MOVE = "MAKE_MOVE"
    private const val START_GAME = "START_GAME"
    private const val END_GAME = "END_GAME"
    private const val INVITE = "INVITE"

    private const val STATE = "STATE"
    private const val MOVE = "MOVE"
    private const val MOVES = "MOVES"
    private const val PROMOTION = "PROMOTION"
    private const val ERROR = "ERROR"
    private const val GAME_ENDED = "GAME_ENDED"
    private const val INVITE_CREATED = "INVITE_CREATED"

    private const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  }

  private val games = ConcurrentHashMap<String, GameService>()
  private val sessionToGame = ConcurrentHashMap<String, String>()
  private val gameSessions = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()
  private val botColors = ConcurrentHashMap<String, Color>()
  private val invitedGames = ConcurrentHashMap.newKeySet<String>()
  private val inviteHostColors = ConcurrentHashMap<String, Color>()

  override fun afterConnectionEstablished(session: WebSocketSession) {
    val requestedGameSessionId = session.queryParam(GAME_SESSION_ID_PARAM)
    val gameSessionId = requestedGameSessionId ?: session.id
    val game = resolveGame(gameSessionId, requestedGameSessionId != null)

    if (game == null) {
      session.sendError("Игра не найдена")
      session.close(CloseStatus.NOT_ACCEPTABLE)
      return
    }

    registerSession(gameSessionId, session)
    if (requestedGameSessionId != null) {
      startInviteGameIfReady(gameSessionId, game)
    }
  }

  override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
    unregisterSession(session)
  }

  override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
    val data = runCatching {
      mapper.readTree(message.payload)
    }.getOrElse {
      session.sendError("Некорректный JSON")
      return
    }

    val type = data.get("type")?.asText()
    val payload = data.get("payload")

    when (type) {
      GET_MOVES -> session.requirePayload(payload)?.let { session.handleGetMoves(it) }
      PROMOTE -> session.requirePayload(payload)?.let { session.handlePromote(it) }
      MAKE_MOVE -> session.requirePayload(payload)?.let { session.handleMove(it) }
      START_GAME -> session.requirePayload(payload)?.let { session.handleStartGame(it) }
      INVITE -> session.requirePayload(payload)?.let { session.handleInvite(it) }
      END_GAME -> session.handleEndGame()
      else -> session.sendError("Неизвестный тип сообщения: $type")
    }
  }

  private fun resolveGame(gameSessionId: String, existingGameOnly: Boolean): GameService? {
    if (existingGameOnly) {
      return games[gameSessionId]
    }

    return games.computeIfAbsent(gameSessionId) {
      gameServiceProvider.getObject()
    }
  }

  private fun registerSession(gameSessionId: String, session: WebSocketSession) {
    sessionToGame[session.id] = gameSessionId

    gameSessions
      .computeIfAbsent(gameSessionId) { ConcurrentHashMap.newKeySet() }
      .add(session)
  }

  private fun unregisterSession(session: WebSocketSession) {
    val gameSessionId = sessionToGame.remove(session.id) ?: return
    val sessions = gameSessions[gameSessionId] ?: return

    sessions.remove(session)

    if (sessions.isEmpty()) {
      gameSessions.remove(gameSessionId)
      if (!invitedGames.contains(gameSessionId)) removeGame(gameSessionId)
    }
  }

  private fun WebSocketSession.handleStartGame(data: JsonNode) {
    val game = game() ?: return
    val gameSessionId = gameSessionId() ?: return
    val playerColor = readColor(data, "color") ?: return
    val botColor = playerColor.opposite()

    synchronized(game) {
      game.loadFromFEN(START_FEN)
      botColors[gameSessionId] = botColor
    }

    sendStateToGame(gameSessionId, game)
    handleBotMove(gameSessionId, game, botColor)
  }

  private fun WebSocketSession.handleInvite(data: JsonNode) {
    val oldGameSessionId = gameSessionId() ?: return
    val game = game() ?: return
    val playerColor = readColor(data, "color") ?: return
    val newGameSessionId = UUID.randomUUID().toString()

    games.remove(oldGameSessionId)
    games[newGameSessionId] = game
    botColors.remove(oldGameSessionId)
    invitedGames.remove(oldGameSessionId)
    invitedGames.add(newGameSessionId)
    inviteHostColors[newGameSessionId] = playerColor
    moveSessionToGame(this, oldGameSessionId, newGameSessionId)

    sendEvent(
      INVITE_CREATED,
      mapOf(
        "gameSessionId" to newGameSessionId,
        "inviteUrl" to buildInviteUrl(newGameSessionId),
        "color" to playerColor,
        "message" to "Ссылка приглашения создана. Ожидаем второго игрока."
      )
    )

    sendEvent("WAITING_FOR_OPPONENT", mapOf("message" to "Ожидаем подключения второго игрока..."))
  }

  private fun WebSocketSession.handleEndGame() {
    val gameSessionId = gameSessionId() ?: return

    sendToGame(
      gameSessionId,
      GAME_ENDED,
      mapOf("message" to "Партия завершена досрочно!")
    )

    removeGame(gameSessionId)
  }

  private fun WebSocketSession.handleGetMoves(data: JsonNode) {
    val game = game() ?: return
    val from = readSquare(data, "from") ?: return
    val result = synchronized(game) {
      game.generateMovesForSquare(from)
    }

    when (result) {
      is AvailableMovesResult.Success -> {
        sendEvent(MOVES, mapOf("moves" to result.moves.map { it.toDto() }))
      }
      is AvailableMovesResult.Error -> {
        sendError(result.message)
      }
    }
  }

  private fun WebSocketSession.handleMove(data: JsonNode) {
    val game = game() ?: return
    val gameSessionId = gameSessionId() ?: return
    val from = readSquare(data, "from") ?: return
    val to = readSquare(data, "to") ?: return
    val botColor = botColors[gameSessionId]

    val result = synchronized(game) {
      game.makeMove(from, to)
    }

    when (result) {
      is MoveResult.Success -> {
        sendMoveAndState(gameSessionId, result.move.toDto(), game)
        if (botColor != null) handleBotMove(gameSessionId, game, botColor)
      }
      is MoveResult.Promotion -> {
        sendEvent(
          PROMOTION,
          mapOf(
            "availablePieces" to result.availablePieces,
            "move" to result.move.toDto()
          )
        )
      }
      is MoveResult.Error -> {
        sendError(result.message)
      }
    }
  }

  private fun WebSocketSession.handlePromote(data: JsonNode) {
    val game = game() ?: return
    val gameSessionId = gameSessionId() ?: return
    val from = readSquare(data, "from") ?: return
    val to = readSquare(data, "to") ?: return
    val piece = data.get("piece")?.asText()

    if (piece.isNullOrBlank()) {
      sendError("Не выбрана фигура для превращения")
      return
    }

    val botColor = botColors[gameSessionId]
    val result = synchronized(game) {
      game.promote(from, to, piece)
    }

    when (result) {
      is PromotionResult.Success -> {
        sendMoveAndState(gameSessionId, result.move.toDto(), game)
        if (botColor != null) handleBotMove(gameSessionId, game, botColor)
      }
      is PromotionResult.Error -> {
        sendError(result.message)
      }
    }
  }

  private fun WebSocketSession.handleBotMove(gameSessionId: String, game: GameService, botColor: Color) {
    if (game.currentTurn != botColor) {
      return
    }

    val bestMove = bot.findBestMove(game) ?: return
    val from = BoardUtils.toSquare(bestMove.from)
    val to = BoardUtils.toSquare(bestMove.to)
    val result = synchronized(game) {
      game.makeMove(from, to)
    }

    when (result) {
      is MoveResult.Success -> {
        sendMoveAndState(gameSessionId, result.move.toDto(), game)
      }
      is MoveResult.Promotion -> {
        handleBotPromotion(gameSessionId, game, from, to)
      }
      is MoveResult.Error -> {
        sendError(result.message)
      }
    }
  }

  private fun WebSocketSession.handleBotPromotion(gameSessionId: String, game: GameService, from: Int, to: Int) {
    val result = synchronized(game) {
      game.promote(from, to, "Queen")
    }

    when (result) {
      is PromotionResult.Success -> {
        sendMoveAndState(gameSessionId, result.move.toDto(), game)
      }
      is PromotionResult.Error -> {
        sendError(result.message)
      }
    }
  }

  private fun moveSessionToGame(
    session: WebSocketSession,
    oldGameSessionId: String,
    newGameSessionId: String
  ) {
    gameSessions[oldGameSessionId]?.remove(session)
    if (gameSessions[oldGameSessionId]?.isEmpty() == true) {
      gameSessions.remove(oldGameSessionId)
    }

    sessionToGame[session.id] = newGameSessionId
    gameSessions
      .computeIfAbsent(newGameSessionId) { ConcurrentHashMap.newKeySet() }
      .add(session)
  }

  private fun removeGame(gameSessionId: String) {
    games.remove(gameSessionId)
    botColors.remove(gameSessionId)
    invitedGames.remove(gameSessionId)

    gameSessions.remove(gameSessionId)
      ?.forEach { session -> sessionToGame.remove(session.id) }
  }

  private fun WebSocketSession.gameSessionId(): String? = sessionToGame[id]

  private fun WebSocketSession.game(): GameService? = gameSessionId()?.let { games[it] }

  private fun buildStatePayload(game: GameService): Map<String, Any?> =
    mapOf(
      "pieces" to game.getPieces().map { it.toDto() },
      "turn" to game.currentTurn,
      "state" to game.getGameState()
    )

  private fun sendMoveAndState(gameSessionId: String, move: Any, game: GameService) {
    sendToGame(gameSessionId, MOVE, mapOf("move" to move))
    sendStateToGame(gameSessionId, game)
  }

  private fun sendStateToGame(gameSessionId: String, game: GameService) {
    sendToGame(gameSessionId, STATE, buildStatePayload(game))
  }

  private fun sendToGame(gameSessionId: String, type: String, payload: Any) {
    gameSessions[gameSessionId]
      ?.filter { it.isOpen }
      ?.forEach { session ->
        session.sendEvent(type, payload)
      }
  }

  private fun WebSocketSession.sendError(message: String) {
    sendEvent(ERROR, mapOf("message" to message))
  }

  private fun WebSocketSession.sendEvent(type: String, payload: Any) {
    if (!isOpen) {
      return
    }

    val json = mapper.writeValueAsString(
      mapOf(
        "type" to type,
        "payload" to payload
      )
    )

    runCatching {
      sendMessage(TextMessage(json))
    }
  }

  private fun WebSocketSession.requirePayload(payload: JsonNode?): JsonNode? {
    if (payload == null || payload.isNull) {
      sendError("Payload отсутствует")
      return null
    }

    return payload
  }

  private fun WebSocketSession.readSquare(data: JsonNode, field: String): Int? {
    val value = data.get(field)?.asText()

    if (value.isNullOrBlank()) {
      sendError("Не указана клетка: $field")
      return null
    }

    return runCatching {
      BoardUtils.toSquare(value)
    }.getOrElse {
      sendError("Некорректная клетка: $value")
      null
    }
  }

  private fun WebSocketSession.readColor(data: JsonNode, field: String): Color? {
    val value = data.get(field)?.asText()

    if (value.isNullOrBlank()) {
      sendError("Не указан цвет")
      return null
    }

    return runCatching {
      Color.valueOf(value)
    }.getOrElse {
      sendError("Некорректный цвет: $value")
      null
    }
  }

  private fun WebSocketSession.queryParam(name: String): String? =
    uri
      ?.rawQuery
      ?.split("&")
      ?.asSequence()
      ?.mapNotNull { rawParam ->
        val parts = rawParam.split("=", limit = 2)

        if (parts.size != 2) {
          return@mapNotNull null
        }

        val key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8)
        val value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8)

        key to value
      }
      ?.firstOrNull { it.first == name }
      ?.second

  private fun WebSocketSession.buildInviteUrl(gameSessionId: String): String {
    val origin = handshakeHeaders.origin?.trimEnd('/')

    if (origin.isNullOrBlank()) {
      return "/chess?gameSessionId=$gameSessionId"
    }

    return "$origin/chess?gameSessionId=$gameSessionId"
  }

  private fun startInviteGameIfReady(gameSessionId: String, game: GameService) {
    if (!invitedGames.contains(gameSessionId)) {
      sendStateToGame(gameSessionId, game)
      return
    }

    val sessionsCount = gameSessions[gameSessionId] ?.count { it.isOpen } ?: 0
    if (sessionsCount < 2) {
      return
    }

    synchronized(game) {
      game.loadFromFEN(START_FEN)
    }

    sendToGame(gameSessionId, "TOAST", mapOf(
      "message" to "Второй игрок подключился. Партия началась!",
      "type" to "success"
    ))

    sendStateToGame(gameSessionId, game)
  }
}