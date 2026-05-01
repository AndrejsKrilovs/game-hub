package krilovs.andrejs.chess.piece

import krilovs.andrejs.chess.game.BoardService

class Pawn(color: Color, square: Int) : Piece(color, square) {
  private val dir = if (color == Color.WHITE) 8 else -8
  private val startRank = if (color == Color.WHITE) 1 else 6

  override fun generateAvailableMoves(board: BoardService): Set<Int> =
    buildSet {
      (square + dir).takeIf { it.isFree(board) }?.let(::add)
      (square + dir * 2).takeIf { square / 8 == startRank && it.isFree(board) }?.let(::add)

      diagonalTargets().filter { it.enemyAt(board) }.forEach(::add)
    }

  override fun generateAttacks(board: BoardService): Set<Int> = diagonalTargets().toSet()

  private fun diagonalTargets() = sequenceOf(square + dir - 1, square + dir + 1)
    .filter { it.isValidDiagonalFrom(square) }

  private fun Int.isValidDiagonalFrom(from: Int) =
    isInsideBoard(this) && kotlin.math.abs(file(from) - file(this)) == 1

  private fun Int.isFree(board: BoardService) =
    isInsideBoard(this) && board[this] == null

  private fun Int.enemyAt(board: BoardService) =
    isInsideBoard(this) && board[this]?.color != color
}