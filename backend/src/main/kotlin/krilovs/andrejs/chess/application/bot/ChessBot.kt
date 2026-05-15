package krilovs.andrejs.chess.application.bot

import krilovs.andrejs.chess.application.GameService
import krilovs.andrejs.chess.domain.model.Color
import krilovs.andrejs.chess.domain.model.Move
import krilovs.andrejs.chess.utils.BoardUtils
import org.springframework.stereotype.Component

@Component
class ChessBot(
  private val game: GameService,
  private val evaluation: EvaluationService
) {

  fun findBestMove(): Move? = alphaBeta(depth = 2, alpha = -INF, beta = INF, botColor = game.currentTurn).move

  private fun alphaBeta(depth: Int, alpha: Int, beta: Int, botColor: Color): ScoredMove {
    if (depth == 0) {
      return ScoredMove(move = null, score = evaluatePosition(botColor))
    }

    val moves = game.getAllMoves()

    if (moves.isEmpty()) {
      return ScoredMove(move = null, score = evaluatePosition(botColor))
    }

    var a = alpha
    var b = beta
    val maximizing = game.currentTurn == botColor
    var bestMove: Move? = null
    var bestScore = if (maximizing) -INF else INF

    for (move in moves) {
      val undo = game.makeMoveInternal(BoardUtils.toSquare(move.from), BoardUtils.toSquare(move.to))
      val score = alphaBeta(depth = depth - 1, alpha = a, beta = b, botColor = botColor).score
      game.undoMove(undo)

      if (maximizing) {
        if (score > bestScore) {
          bestScore = score
          bestMove = move
        }

        a = maxOf(a, bestScore)

      }
      else {
        if (score < bestScore) {
          bestScore = score
          bestMove = move
        }

        b = minOf(b, bestScore)
      }
      if (a >= b) {
        break
      }
    }

    return ScoredMove(move = bestMove, score = bestScore)
  }

  private fun evaluatePosition(botColor: Color): Int = evaluation.evaluate(game.getBoardCopy(), botColor)

  companion object {
    private const val INF = 1_000_000_000
  }
}