package krilovs.andrejs.chess.application.bot

import krilovs.andrejs.chess.application.GameService
import krilovs.andrejs.chess.domain.model.Move
import krilovs.andrejs.chess.utils.BoardUtils
import org.springframework.stereotype.Component

@Component
class ChessBot(
  private val game: GameService,
  private val search: SearchService,
  private val properties: BotProperties
) {

  fun findBestMove(): Move? {
    search.reset()

    val result = search.searchBestMove(
      maxDepth = properties.maxDepth,
      timeLimitMs = properties.timeLimitMs,
      botColor = game.currentTurn
    )

    val move = result.move ?: return null
    val piece = game.getPiece(move.from) ?: return null

    return Move(
      from = BoardUtils.toCord(move.from),
      to = BoardUtils.toCord(move.to),
      piece = piece
    )
  }
}