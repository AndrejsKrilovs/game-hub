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

  override fun afterConnectionEstablished(session: WebSocketSession) {
    sessions += session
    session.sendState("INIT")
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
    sendJson("MOVES", mapOf("moves" to moves))
  }

  private fun WebSocketSession.handleMove(data: JsonNode) {
    val from = data["from"]?.asText()?.toSquare() ?: return
    val to = data["to"]?.asText()?.toSquare() ?: return
    val moves = board.generateMovesForSquare(from)

    val move = moves.firstOrNull { it.to == to }
      ?: return sendJson(
        "INVALID_MOVE",
        mapOf("availableMoves" to moves.map { it.to.toCoord() })
      )

    board.makeMove(move)
  }

  private fun WebSocketSession.sendState(type: String) {
    sendJson(type, buildStatePayload())
  }

  private fun buildStatePayload() = mapOf(
    "pieces" to board.pieces.map { it.toDto() },
    "turn" to board.currentTurn.name,
    "state" to rules.getGameState(board.currentTurn).name
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

  private fun WebSocketSession.sendJson(type: String, payload: Map<String, Any?>) {
    val json = mapper.writeValueAsString(payload + ("type" to type))
    sendMessage(TextMessage(json))
  }
}