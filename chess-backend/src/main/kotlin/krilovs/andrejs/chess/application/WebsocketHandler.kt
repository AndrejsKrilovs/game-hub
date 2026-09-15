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
import java.util.concurrent.ConcurrentHashMap

@Component
class WebsocketHandler(
  private val mapper: ObjectMapper,
  private val gameServiceProvider: ObjectProvider<GameService>,
  private val bot: ChessBot
) : TextWebSocketHandler() {

  private val games = ConcurrentHashMap<String, GameService>()
  private val botColors = ConcurrentHashMap<String, Color>()
  private val startFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

  override fun afterConnectionEstablished(session: WebSocketSession) {
    games[session.id] = gameServiceProvider.getObject()
  }

  override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
    games.remove(session.id)
    botColors.remove(session.id)
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
    val game = games[id] ?: return
    game.loadFromFEN(startFEN)

    val botColor = Color.valueOf(data["color"].asText()).opposite()
    botColors[id] = botColor

    sendEvent("STATE", buildStatePayload(game))
    handleBotMove(game, botColor)
  }

  private fun WebSocketSession.handleEndGame() {
    sendEvent("GAME_ENDED", mapOf("message" to "Партия завершена досрочно!"))
  }

  private fun WebSocketSession.handleGetMoves(data: JsonNode) {
    val game = games[id] ?: return
    val result = game.generateMovesForSquare(data.square("from"))
    val (type, payload) = when (result) {
      is AvailableMovesResult.Success -> "MOVES" to mapOf("moves" to result.moves.map { it.toDto() })
      is AvailableMovesResult.Error -> "ERROR" to mapOf("message" to result.message)
    }

    sendEvent(type, payload)
  }

  private fun WebSocketSession.handleMove(data: JsonNode) {
    val game = games[id] ?: return
    val botColor = botColors[id] ?: Color.BLACK
    val from = data.square("from")
    val to = data.square("to")

    when (val result = game.makeMove(from, to)) {
      is MoveResult.Success -> {
        sendEvent("MOVE", mapOf("move" to result.move.toDto()))
        sendEvent("STATE", buildStatePayload(game))
        handleBotMove(game, botColor)
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
    val game = games[id] ?: return
    val botColor = botColors[id] ?: Color.BLACK
    val to = data.square("to")
    val piece = data["piece"].asText()
    val from = data.square("from")

    when (val result = game.promote(from, to, piece)) {
      is PromotionResult.Success -> {
        sendEvent("MOVE", mapOf("move" to result.move.toDto()))
        sendEvent("STATE", buildStatePayload(game))
        handleBotMove(game, botColor)
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

  private fun WebSocketSession.handleBotMove(game: GameService, botColor: Color) {
    if (game.currentTurn != botColor) return
    val bestMove = bot.findBestMove(game) ?: return
    val from = BoardUtils.toSquare(bestMove.from)
    val to = BoardUtils.toSquare(bestMove.to)

    when (val result = game.makeMove(from, to)) {
      is MoveResult.Success -> {
        sendEvent("MOVE", mapOf("move" to result.move.toDto()))
        sendEvent("STATE", buildStatePayload(game))
      }
      is MoveResult.Promotion -> {
        when (val promotion = game.promote(from, to, "Queen")) {
          is PromotionResult.Success -> {
            sendEvent("MOVE", mapOf("move" to promotion.move.toDto()))
            sendEvent("STATE", buildStatePayload(game))
          }
          is PromotionResult.Error -> {
            sendEvent("ERROR", mapOf("message" to promotion.message))
          }
        }
      }
      is MoveResult.Error -> {
        sendEvent("ERROR", mapOf("message" to result.message))
      }
    }
  }

  private fun buildStatePayload(game: GameService) = mapOf(
    "pieces" to game.getPieces().map { it.toDto() },
    "turn" to game.currentTurn,
    "state" to game.getGameState()
  )

  private fun JsonNode.square(field: String): Int =
    BoardUtils.toSquare(this[field].asText())
}