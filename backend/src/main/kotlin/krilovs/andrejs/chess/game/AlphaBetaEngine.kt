package krilovs.andrejs.chess.game

import krilovs.andrejs.chess.piece.Bishop
import krilovs.andrejs.chess.piece.King
import krilovs.andrejs.chess.piece.Knight
import krilovs.andrejs.chess.piece.Pawn
import krilovs.andrejs.chess.piece.Piece
import krilovs.andrejs.chess.piece.Queen
import krilovs.andrejs.chess.piece.Rook
import kotlin.collections.sumOf

class AlphaBetaEngine(
  private val board: Board,
  private val rules: GameRules
) {

  fun findBestMove(depth: Int): Move? {
    var bestMove: Move? = null
    var bestScore = -INF
    val moves = board.generateMoves().toMutableList().apply(::orderMoves)

    for (move in moves) {
      if (!rules.isMoveSafe(move)) continue
      board.makeMove(move)

      val score = -alphaBeta(depth - 1, -INF, INF)
      board.unmakeMove(move)
      if (score > bestScore) {
        bestScore = score
        bestMove = move
      }
    }

    return bestMove
  }

  private fun alphaBeta(depth: Int, alphaInit: Int, betaInit: Int): Int {
    var alpha = alphaInit
    when (rules.getGameState(board.currentTurn)) {
      GameState.CHECKMATE -> return -MATE
      GameState.STALEMATE -> return 0
      else -> {}
    }

    if (depth == 0) return evaluate()
    val moves = board.generateMoves().toMutableList().apply(::orderMoves)
    for (move in moves) {
      if (!rules.isMoveSafe(move)) continue
      board.makeMove(move)
      val score = -alphaBeta(depth - 1, -betaInit, -alpha)
      board.unmakeMove(move)

      if (score >= betaInit) return betaInit
      if (score > alpha) alpha = score
    }

    return alpha
  }

  private fun evaluate(): Int {
    val score = board.pieces.sumOf { piece ->
      val value = pieceValue(piece) + positionBonus(piece)
      if (piece.color == Color.WHITE) value else -value
    }

    return if (board.currentTurn == Color.WHITE) score else -score
  }

  private fun positionBonus(piece: Piece): Int {
    return if (piece.square in setOf(27, 28, 35, 36)) {
      when (piece) {
        is Pawn -> 20
        is Knight, is Bishop -> 30
        else -> 0
      }
    } else 0
  }

  private fun orderMoves(moves: MutableList<Move>): Unit =
    moves.sortByDescending { score(it) + moveScore(it) }

  // MVV-LVA
  private fun score(m: Move): Int =
    m.captured?.let { 1000 + pieceValue(it) - pieceValue(m.piece) } ?: 0

  private fun pieceValue(piece: Piece): Int = when (piece) {
    is Pawn -> 100
    is Knight, is Bishop -> 300
    is Rook -> 500
    is Queen -> 900
    is King -> 10000
    else -> 0
  }

  private fun promotionBonus(move: Move): Int {
    return when (move.promotion) {
      'q' -> 900
      'r' -> 500
      'b', 'n' -> 300
      else -> 0
    }
  }

  private fun moveScore(move: Move): Int =
    (move.captured?.let { pieceValue(it) * 10 } ?: 0) + promotionBonus(move)

  companion object {
    private const val INF = Int.MAX_VALUE - 1
    private const val MATE = 100_000
  }
}