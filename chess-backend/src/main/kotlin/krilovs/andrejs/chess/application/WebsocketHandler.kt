package krilovs.andrejs.chess.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import krilovs.andrejs.chess.application.bot.ChessBot
import krilovs.andrejs.chess.domain.model.Color
import krilovs.andrejs.chess.dto.AvailableMovesResult
import krilovs.andrejs.chess.dto.MoveResult
import krilovs.andrejs.chess.dto.PromotionResult
import krilovs.andrejs.chess.utils.BoardUtils
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class WebsocketHandler(
  private val mapper: ObjectMapper,
  private val game: GameService,
  private val bot: ChessBot
) : TextWebSocketHandler() {
  private var botColor = Color.BLACK
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
      "PROMOTE" -> session.handlePromote(payload)
      "MAKE_MOVE" -> session.handleMove(payload)
      "START_GAME" -> session.handleStartGame(payload)
      "END_GAME" -> session.handleEndGame()
    }
  }

  private fun WebSocketSession.handleStartGame(data: JsonNode) {
    game.loadFromFEN(startFEN)
    botColor = Color.valueOf(data["color"].asText()).opposite()

    sendEvent("STATE", buildStatePayload())
    handleBotMove()
  }

  private fun WebSocketSession.handleEndGame() {
    sendEvent("GAME_ENDED", mapOf("message" to "Партия завершена досрочно!"))
  }

  private fun WebSocketSession.handleGetMoves(data: JsonNode) {
    val result = game.generateMovesForSquare(data.square("from"))
    val (type, payload) = when (result) {
      is AvailableMovesResult.Success -> "MOVES" to mapOf("moves" to result.moves.map { it.toDto() })
      is AvailableMovesResult.Error -> "ERROR" to mapOf("message" to result.message)
    }

    sendEvent(type, payload)
  }

  private fun WebSocketSession.handleMove(data: JsonNode) {
    val from = data.square("from")
    val to = data.square("to")

    when (val result = game.makeMove(from, to)) {
      is MoveResult.Success -> {
        sendEvent("MOVE", mapOf("move" to result.move.toDto()))
        sendEvent("STATE", buildStatePayload())
        handleBotMove()
      }
      is MoveResult.Error -> {
        sendEvent("ERROR", mapOf("message" to result.message))
      }
      is MoveResult.Promotion -> {
        sendEvent(
          "PROMOTION",
          mapOf("availablePieces" to result.availablePieces, "move" to result.move.toDto())
        )
      }
    }
  }

  private fun WebSocketSession.handlePromote(data: JsonNode) {
    val to = data.square("to")
    val piece = data["piece"].asText()
    val from = data.square("from")

    when (val result = game.promote(from, to, piece)) {
      is PromotionResult.Success -> {
        sendEvent("MOVE", mapOf("move" to result.move.toDto()))
        sendEvent("STATE", buildStatePayload())
        handleBotMove()
      }
      is PromotionResult.Error -> {
        sendEvent("ERROR", mapOf("message" to result.message))
      }
    }
  }

  private fun WebSocketSession.sendEvent(type: String, payload: Any) {
    val json = mapper.writeValueAsString(mapOf("type" to type, "payload" to payload))
    sendMessage(TextMessage(json))
  }

  private fun WebSocketSession.handleBotMove() {
    val move = bot.takeIf { game.currentTurn == botColor }
      ?.findBestMove()
      ?.also { game.makeMove(BoardUtils.toSquare(it.from), BoardUtils.toSquare(it.to)) }

    sendEvent("MOVE", mapOf("move" to (move?.toDto() ?: return)))
    sendEvent("STATE", buildStatePayload())
  }

  private fun buildStatePayload() = mapOf(
    "pieces" to game.getPieces().map { it.toDto() },
    "turn" to game.currentTurn,
    "state" to game.getGameState()
  )

  private fun JsonNode.square(field: String): Int =
    BoardUtils.toSquare(this[field].asText())
}