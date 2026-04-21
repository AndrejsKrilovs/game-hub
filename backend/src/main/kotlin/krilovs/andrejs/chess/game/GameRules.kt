package krilovs.andrejs.chess.game

import krilovs.andrejs.chess.piece.King
import krilovs.andrejs.chess.piece.Pawn

class GameRules(
  private val board: Board,
  private val attackService: AttackService
) {

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

  fun getGameState(color: Color): GameState {
    val kingSq = findKing(color)
    val inCheck = attackService.isSquareUnderAttack(kingSq, color.opposite())
    val hasMoves = board.generateMoves().any(::isMoveSafe)

    return when {
      inCheck && !hasMoves -> GameState.CHECKMATE
      !inCheck && !hasMoves -> GameState.STALEMATE
      inCheck -> GameState.CHECK
      else -> GameState.NORMAL
    }
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

  private fun findKing(color: Color): Int =
    board.pieces.first { it is King && it.color == color }.square
}