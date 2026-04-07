package krilovs.andrejs.chess.game

import krilovs.andrejs.chess.piece.Bishop
import krilovs.andrejs.chess.piece.King
import krilovs.andrejs.chess.piece.Knight
import krilovs.andrejs.chess.piece.Pawn
import krilovs.andrejs.chess.piece.Piece
import krilovs.andrejs.chess.piece.Queen
import krilovs.andrejs.chess.piece.Rook

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
    val material = board.pieces.sumOf { piece ->
      val value = pieceValue(piece) + positionBonus(piece)
      if (piece.color == board.currentTurn) value else -value
    }

    val mobility = board.generateMoves().size * MOBILITY_BONUS
    return material + mobility
  }

  private fun positionBonus(piece: Piece): Int {
    val center = setOf(27, 28, 35, 36) // d4 e4 d5 e5
    return if (piece.square in center) {
      when (piece) {
        is Pawn -> 20
        is Knight, is Bishop -> 30
        else -> 0
      }
    } else 0
  }

  private fun orderMoves(moves: MutableList<Move>): Unit =
    moves.sortByDescending(::score)

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

  companion object {
    private const val INF = Int.MAX_VALUE - 1
    private const val MATE = 100_000
    private const val MOBILITY_BONUS = 5
  }
}