package krilovs.andrejs.chess.application.bot

import krilovs.andrejs.chess.application.GameService
import krilovs.andrejs.chess.domain.model.Color
import krilovs.andrejs.chess.domain.model.Move
import krilovs.andrejs.chess.domain.piece.Bishop
import krilovs.andrejs.chess.domain.piece.King
import krilovs.andrejs.chess.domain.piece.Knight
import krilovs.andrejs.chess.domain.piece.Pawn
import krilovs.andrejs.chess.domain.piece.Piece
import krilovs.andrejs.chess.domain.piece.Queen
import krilovs.andrejs.chess.domain.piece.Rook
import krilovs.andrejs.chess.utils.BoardUtils
import org.springframework.stereotype.Component

@Component
class ChessBot(
  private val game: GameService,
  private val evaluation: EvaluationService
) {

  fun findBestMove(): Move? = alphaBeta(depth = 3, alpha = -INF, beta = INF, botColor = game.currentTurn).move

  private fun alphaBeta(depth: Int, alpha: Int, beta: Int, botColor: Color): ScoredMove {
    if (depth == 0) {
      return ScoredMove(move = null, score = evaluatePosition(botColor))
    }

    val moves = orderedMoves()
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

  private fun evaluatePosition(botColor: Color): Int = evaluation.evaluate(game.getBoard(), botColor)

  private fun orderedMoves(): List<Move> = game.getAllMoves().sortedByDescending { move ->
      game.getPiece(BoardUtils.toSquare(move.to))?.let { pieceValue(it) } ?: 0
    }

  private fun pieceValue(piece: Piece): Int =
    when (piece) {
      is Queen -> 900
      is Rook -> 500
      is Bishop -> 330
      is Knight -> 320
      is Pawn -> 100
      is King -> 20_000
      else -> 0
    }
  companion object {
    private const val INF = Int.MAX_VALUE - 1
  }
}