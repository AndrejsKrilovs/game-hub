package krilovs.andrejs.chess.application.bot

import krilovs.andrejs.chess.application.GameService
import krilovs.andrejs.chess.domain.model.Move
import krilovs.andrejs.chess.utils.BoardUtils
import org.springframework.stereotype.Component

@Component
class ChessBot(
  private val game: GameService,
  private val evaluation: EvaluationService
) {
  fun findBestMove(): Move? {
    val moves = game.getAllMoves()
    if (moves.isEmpty()) return null

    return moves.maxByOrNull { move ->
      val from = BoardUtils.toSquare(move.from)
      val to = BoardUtils.toSquare(move.to)
      val undo = game.makeMoveInternal(from, to)

      val score = evaluation.evaluate(
        game.getBoardCopy(),
        game.currentTurn.opposite()
      )

      game.undoMove(undo)
      score
    }
  }
}