package krilovs.andrejs.chess.game

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import krilovs.andrejs.chess.piece.Piece
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

class Handler : TextWebSocketHandler() {

  private val mapper = jacksonObjectMapper()
  private val sessions = mutableSetOf<WebSocketSession>()

  private val board = Board().apply {
    loadFromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
  }

  private val attackService = AttackService(board)
  private val rules = GameRules(board, attackService)
  private val engine = AlphaBetaEngine(board, rules)

  private val botColor = Color.BLACK
  private var lastMove: Move? = null

  override fun afterConnectionEstablished(session: WebSocketSession) {
    sessions += session
    session.sendEvent("INIT", buildStatePayload())

    makeBotMoveIfNeeded()?.let {
      broadcastEvent("MOVE", it.toDto())
      broadcastState()
    }
  }

  override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
    sessions -= session
  }

  override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
    val data = mapper.readTree(message.payload)
    when (data["type"]?.asText()) {
      "GET_MOVES" -> session.handleGetMoves(data)
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

    board.makeMove(move)
    lastMove = move
    broadcastEvent("MOVE", move.toDto())
    makeBotMoveIfNeeded()?.let { broadcastEvent("MOVE", it.toDto()) }
    broadcastState()
  }

  private fun makeBotMoveIfNeeded(): Move? {
    if (board.currentTurn != botColor) return null

    val move = engine.findBestMove(3)
    if (move != null) {
      board.makeMove(move)
      lastMove = move
    }
    return move
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

  private fun Piece.toDto() = mapOf(
    "type" to type,
    "color" to color.name,
    "coordinates" to mapOf(
      "file" to ('a' + (square % 8)).toString(),
      "rank" to (square / 8) + 1
    )
  )

  private fun Int.toCoord() = "${'a' + (this % 8)}${(this / 8) + 1}"
  private fun String.toSquare(): Int = (this[1].digitToInt() - 1) * 8 + (this[0] - 'a')
}