package krilovs.andrejs.chess.game

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import krilovs.andrejs.chess.dto.AvailableMovesResult
import krilovs.andrejs.chess.dto.MoveResult
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class Handler(
  private val mapper: ObjectMapper,
  private val board: BoardService
) : TextWebSocketHandler() {
  private val sessions = mutableSetOf<WebSocketSession>()
  private val startFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

  override fun afterConnectionEstablished(session: WebSocketSession) {
    sessions += session
  }

  override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
    sessions -= session
  }

  override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
    val data = mapper.readTree(message.payload)
    val payload = data["payload"]
    val type = data["type"]?.asText()

    when (type) {
      "GET_MOVES" -> session.handleGetMoves(payload)
      "MAKE_MOVE" -> session.handleMove(payload)
      "START_GAME" -> session.handleStartGame()
      "END_GAME" -> session.handleEndGame()
    }
  }

  private fun WebSocketSession.handleStartGame() {
    board.loadFromFEN(startFEN)
    sendEvent("STATE", buildStatePayload())
  }

  private fun WebSocketSession.handleEndGame() {
    sendEvent("GAME_ENDED", mapOf("message" to "Партия завершена досрочно"))
  }

  private fun WebSocketSession.handleGetMoves(data: JsonNode) {
    val from = BoardUtils.toSquare(data["from"].asText())
    val result = board.generateMovesForSquare(from)
    val (type, payload) = when (result) {
      is AvailableMovesResult.Success -> "MOVES" to mapOf("moves" to result.moves.map { it.toDto() })
      is AvailableMovesResult.Error -> "ERROR" to mapOf("message" to result.message)
    }

    sendEvent(type, payload)
  }

  private fun WebSocketSession.handleMove(data: JsonNode) {
    val from = BoardUtils.toSquare(data["from"].asText())
    val to = BoardUtils.toSquare(data["to"].asText())
    val result = board.makeMove(from, to)
    val (type, payload) = when (result) {
      is MoveResult.Success -> "MOVE" to mapOf("move" to result.move.toDto())
      is MoveResult.Error -> "ERROR" to mapOf("message" to result.message)
    }

    sendEvent(type, payload)
    sendEvent("STATE", buildStatePayload())
  }

  private fun WebSocketSession.sendEvent(type: String, payload: Any) {
    val json = mapper.writeValueAsString(mapOf("type" to type, "payload" to payload))
    sendMessage(TextMessage(json))
  }

  private fun buildStatePayload() = mapOf(
    "pieces" to board.pieces.map { it.toDto() },
    "turn" to board.currentColor,
    "state" to board.getGameState()
  )
}