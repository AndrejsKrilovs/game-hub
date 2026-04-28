package krilovs.andrejs.chess.game

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

class Handler : TextWebSocketHandler() {
  private val startFEN = "4K/PPB/8/8/8/8/ppppn2P/k7 b KQkq - 0 1"
  private val mapper = jacksonObjectMapper()
  private val sessions = mutableSetOf<WebSocketSession>()
  private val board = Board().apply { reset() }

  private val attackService = AttackService(board)
  private val rules = GameRules(board, attackService)
  private val engine = AlphaBetaEngine(board, rules)

  private var botColor = Color.BLACK
  private var pendingMove: Move? = null
  private val positionHistory = mutableMapOf<String, Int>()

  init {
    rules.isThreefoldRepetition = { isThreefoldRepetition() }
  }

  override fun afterConnectionEstablished(session: WebSocketSession) {
    sessions += session
  }

  override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
    sessions -= session
  }

  override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
    val data = mapper.readTree(message.payload)
    val type = data["type"]?.asText()
    val payload = data["payload"]

    when (type) {
      "START_GAME" -> session.handleStartGame(payload)
      "GET_MOVES" -> session.handleGetMoves(payload)
      "PROMOTE" -> session.handlePromote(payload)
      "MAKE_MOVE" -> session.handleMove(payload)
      "END_GAME" -> session.handleEndGame()
    }
  }

  private fun WebSocketSession.handleGetMoves(data: JsonNode) {
    val from = data["from"]?.asText()?.toSquare() ?: return
    val piece = board[from] ?: return sendEvent("ERROR", mapOf("message" to "Пустая клетка"))

    if (piece.color != board.currentTurn) {
      return sendEvent("ERROR", mapOf("message" to "Фигура другого цвета"))
    }

    board.generateMovesForSquare(from)
      .takeIf { it.isNotEmpty() }
      ?.map { it.to.toCoord() }
      ?.also { sendEvent("MOVES", mapOf("moves" to it)) }
      ?: sendEvent("ERROR", mapOf("message" to "Нет доступных ходов"))
  }

  private fun WebSocketSession.handleMove(data: JsonNode) {
    val from = data["from"]?.asText()?.toSquare() ?: return
    val to = data["to"]?.asText()?.toSquare() ?: return
    val moves = board.generateMovesForSquare(from)

    val move = moves.firstOrNull { it.to == to }
      ?: return sendEvent(
        "ERROR",
        mapOf(
          "message" to moves.joinToString(", ", "Некорректный ход\nДоступные ходы: ") { it.to.toCoord() }
        )
      )

    if (rules.isPromotion(move)) {
      pendingMove = move
      return sendEvent(
        "PROMOTION",
        mapOf(
          "availablePieces" to listOf("Queen", "Rook", "Bishop", "Knight"),
          "color" to move.piece.color.name
        )
      )
    }

    applyMove(move)
  }

  private fun WebSocketSession.handlePromote(data: JsonNode) {
    val move = pendingMove ?: return
    val pieceName = data["piece"]?.asText() ?: return

    val promotionChar = when (pieceName) {
      "Queen" -> 'q'
      "Rook" -> 'r'
      "Bishop" -> 'b'
      "Knight" -> 'n'
      else -> return
    }.let { if (move.piece.color == Color.WHITE) it.uppercaseChar() else it }

    val promotedMove = move.copy(promotion = promotionChar)
    pendingMove = null
    applyMove(promotedMove)
  }

  private fun WebSocketSession.handleStartGame(data: JsonNode) {
    board.reset()
    positionHistory.clear()
    recordPosition()

    pendingMove = null
    botColor = Color.valueOf(data["color"].asText()).opposite()
    makeBotMoveIfNeeded()?.let {
      recordPosition()
      sendEvent("MOVE", it.toDto())
    }
    sendEvent("STATE", buildStatePayload())
  }

  private fun WebSocketSession.handleEndGame() {
    board.reset()
    pendingMove = null
    sendEvent("GAME_ENDED", mapOf("message" to "Партия завершена досрочно"))
  }

  private fun WebSocketSession.sendEvent(type: String, payload: Any?) {
    val json = mapper.writeValueAsString(mapOf("type" to type, "payload" to payload))
    sendMessage(TextMessage(json))
  }

  private fun applyMove(move: Move) {
    board.makeMove(move)
    recordPosition()
    broadcastEvent("MOVE", move.toDto())
    makeBotMoveIfNeeded()?.let {
      recordPosition()
      broadcastEvent("MOVE", it.toDto())
    }
    broadcastState()
  }

  private fun makeBotMoveIfNeeded(): Move? =
    engine.takeIf { board.currentTurn == botColor }
      ?.findBestMove(4)
      ?.also { board.makeMove(it) }

  private fun broadcastEvent(type: String, payload: Any?) {
    val json = mapper.writeValueAsString(mapOf("type" to type, "payload" to payload))
    sessions.forEach { it.sendMessage(TextMessage(json)) }
  }

  private fun buildStatePayload() = mapOf(
    "pieces" to board.pieces.map { it.toDto() },
    "turn" to board.currentTurn.name,
    "state" to rules.getGameState(board.currentTurn).name
  )

  private fun Move.toDto() = mapOf(
    "from" to from.toCoord(),
    "to" to to.toCoord(),
    "piece" to piece.type,
    "color" to piece.color.name,
    "isCastling" to isCastling,
    "castlingType" to when {
      isCastling && to > from -> "SHORT"
      isCastling && to < from -> "LONG"
      else -> null
    }
  )

  private fun Board.reset() {
    loadFromFEN(startFEN)
  }

  private fun recordPosition() {
    val fen = board.toFEN()
    positionHistory[fen] = (positionHistory[fen] ?: 0) + 1
  }

  private fun isThreefoldRepetition(): Boolean {
    val fen = board.toFEN()
    return (positionHistory[fen] ?: 0) >= 3
  }

  private fun Int.toCoord() = "${'a' + (this % 8)}${(this / 8) + 1}"
  private fun broadcastState() = broadcastEvent("STATE", buildStatePayload())
  private fun String.toSquare(): Int = (this[1].digitToInt() - 1) * 8 + (this[0] - 'a')
}