package krilovs.andrejs.chess.game

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

class Handler : TextWebSocketHandler() {
  private val startFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
  private val mapper = jacksonObjectMapper()
  private val sessions = mutableSetOf<WebSocketSession>()
  private val board = Board().apply { reset() }

  private val attackService = AttackService(board)
  private val rules = GameRules(board, attackService)
  private val engine = AlphaBetaEngine(board, rules)

  private var botColor = Color.BLACK
  private var lastMove: Move? = null

  override fun afterConnectionEstablished(session: WebSocketSession) {
    sessions += session
    session.sendEvent("INIT", buildStatePayload())
  }

  override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
    sessions -= session
  }

  override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
    val data = mapper.readTree(message.payload)
    when (data["type"]?.asText()) {
      "START_GAME" -> session.handleStartGame(data)
      "GET_MOVES" -> session.handleGetMoves(data)
      "PROMOTE" -> session.handlePromote(data)
      "END_GAME" -> session.handleEndGame()
      "MOVE" -> session.handleMove(data)
    }
  }

  private fun WebSocketSession.handleGetMoves(data: JsonNode) {
    val from = data["from"]?.asText()?.toSquare() ?: return
    val moves = board.generateMovesForSquare(from).map { it.to.toCoord() }
    sendEvent("MOVES", mapOf("moves" to moves))
  }

  private fun WebSocketSession.handleMove(data: JsonNode) {
    val from = data["from"]?.asText()?.toSquare() ?: return
    val to = data["to"]?.asText()?.toSquare() ?: return

    val move = board.generateMovesForSquare(from)
      .firstOrNull { it.to == to }
      ?: return sendEvent(
        "INVALID_MOVE",
        mapOf("availableMoves" to board.generateMovesForSquare(from).map { it.to.toCoord() })
      )

    board.makeMove(move.copy(promotion = null))
    lastMove = move

    if (rules.isPromotion(move)) {
      sendEvent("PROMOTION",
        mapOf(
          "availablePieces" to listOf("Queen","Rook","Bishop","Knight"),
          "color" to move.piece.color.name
        )
      )
      return
    }

    broadcastEvent("MOVE", move.toDto())
    makeBotMoveIfNeeded()?.let { broadcastEvent("MOVE", it.toDto()) }
    broadcastState()
  }

  private fun WebSocketSession.handleStartGame(data: JsonNode) {
    board.reset()
    lastMove = null
    botColor = Color.valueOf(data["color"].asText()).opposite()
    makeBotMoveIfNeeded()?.let {
      sendEvent("MOVE", it.toDto())
      sendEvent("STATE", buildStatePayload())
    }
  }

  private fun WebSocketSession.handleEndGame() {
    board.reset()
    lastMove = null
  }

  private fun WebSocketSession.handlePromote(data: JsonNode) {
    val pieceName = data["piece"]?.asText() ?: return
    val move = lastMove ?: return

    val promotionChar = when (pieceName) {
      "Queen" -> 'q'
      "Rook" -> 'r'
      "Bishop" -> 'b'
      "Knight" -> 'n'
      else -> return
    }

    val promotedMove = move.copy(promotion = promotionChar)
    board.makeMove(promotedMove)
    broadcastEvent("MOVE", promotedMove.toDto())
    makeBotMoveIfNeeded()?.let { broadcastEvent("MOVE", it.toDto()) }
    broadcastState()
  }

  private fun makeBotMoveIfNeeded(): Move? =
    engine.takeIf { board.currentTurn == botColor }
      ?.findBestMove(3)
      ?.also {
        board.makeMove(it)
        lastMove = it
      }

  private fun broadcastState() {
    broadcastEvent("STATE", buildStatePayload() + mapOf(
      "lastMove" to lastMove?.toDto()
    ))
  }

  private fun WebSocketSession.sendEvent(type: String, payload: Map<String, Any?>) {
    val json = mapper.writeValueAsString(payload + ("type" to type))
    sendMessage(TextMessage(json))
  }

  private fun broadcastEvent(type: String, payload: Map<String, Any?>) {
    val json = mapper.writeValueAsString(payload + ("type" to type))
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

  private fun Int.toCoord() = "${'a' + (this % 8)}${(this / 8) + 1}"
  private fun String.toSquare(): Int = (this[1].digitToInt() - 1) * 8 + (this[0] - 'a')
}