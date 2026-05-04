package krilovs.andrejs.chess.domain.piece

import krilovs.andrejs.chess.domain.model.Board
import krilovs.andrejs.chess.domain.model.Color

class Pawn(color: Color, square: Int) : Piece(color, square) {
  private val dir = if (color == Color.WHITE) 8 else -8
  private val startRank = if (color == Color.WHITE) 1 else 6

  override fun copy(): Piece = Pawn(color, square)
  override fun generateAvailableMoves(board: Board): Set<Int> =
    buildSet {
      val one = square + dir

      one.takeIf { it.isFree(board) }?.let { first ->
        add(first)

        val two = square + dir * 2
        two.takeIf { square / 8 == startRank && it.isFree(board) }
          ?.let(::add)
      }

      diagonalTargets().filter { it.enemyAt(board) }.forEach(::add)
    }

  override fun generateAttacks(board: Board): Set<Int> = diagonalTargets().toSet()

  private fun diagonalTargets() = sequenceOf(square + dir - 1, square + dir + 1)
    .filter { it.isValidDiagonalFrom(square) }

  private fun Int.isValidDiagonalFrom(from: Int) =
    isInsideBoard(this) && kotlin.math.abs(file(from) - file(this)) == 1

  private fun Int.isFree(board: Board) =
    isInsideBoard(this) && board[this] == null

  private fun Int.enemyAt(board: Board): Boolean {
    val target = board[this] ?: return false
    return target.color != color
  }
}