package krilovs.andrejs.chess.game

import krilovs.andrejs.chess.piece.Bishop
import krilovs.andrejs.chess.piece.King
import krilovs.andrejs.chess.piece.Knight
import krilovs.andrejs.chess.piece.Pawn

class GameRules(
  private val board: Board,
  private val attackService: AttackService
) {
  var isThreefoldRepetition: (() -> Boolean)? = null
  fun isMoveSafe(move: Move): Boolean {
    val enemy = move.piece.color.opposite()

    if (move.isCastling && move.piece is King) {
      val direction = if (move.to > move.from) 1 else -1
      val path = listOf(move.from, move.from + direction, move.to)
      if (path.any { attackService.isSquareUnderAttack(it, enemy) }) {
        return false
      }
    }

    board.makeMove(move)
    val kingSq = findKing(move.piece.color)
    val safe = !attackService.isSquareUnderAttack(kingSq, enemy)
    board.unmakeMove(move)
    return safe
  }

  fun getGameState(color: Color): GameState =
    when {
      isInsufficientMaterial() || isThreefoldRepetition?.invoke() == true -> GameState.STALEMATE
      run {
        val kingSq = findKing(color)
        val inCheck = attackService.isSquareUnderAttack(kingSq, color.opposite())
        val hasMoves = board.generateMoves().any(::isMoveSafe)

        when {
          inCheck && !hasMoves -> return GameState.CHECKMATE
          !inCheck && !hasMoves -> return GameState.STALEMATE
          inCheck -> return GameState.CHECK
          else -> false
        }
      } -> GameState.NORMAL
      else -> GameState.NORMAL
    }

  fun isPromotion(move: Move): Boolean {
    val piece = move.piece
    if (piece !is Pawn) return false

    val targetRank = move.to / 8
    return when (piece.color) {
      Color.WHITE -> targetRank == 7
      Color.BLACK -> targetRank == 0
    }
  }

  private fun isInsufficientMaterial(): Boolean {
    val pieces = board.pieces

    // только короли
    if (pieces.all { it is King }) return true

    // король + лёгкая фигура vs король
    if (pieces.size == 3) {
      return pieces.any { it is Bishop || it is Knight }
    }

    // Король + слон vs король + слон, оба слона ходят по клеткам одного цвета
    val bishops = pieces.filterIsInstance<Bishop>()
    val others = pieces.filterNot { it is King || it is Bishop }

    if (bishops.size == 2 && others.isEmpty()) {
      val colors = bishops.map { squareColor(it.square) }
      return colors.distinct().size == 1
    }

    return false
  }

  private fun squareColor(square: Int): Int {
    val file = square % 8
    val rank = square / 8
    return (file + rank) % 2
  }

  private fun findKing(color: Color): Int =
    board.pieces.first { it is King && it.color == color }.square
}