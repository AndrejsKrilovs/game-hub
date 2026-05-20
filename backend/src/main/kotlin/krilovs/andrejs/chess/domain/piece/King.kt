package krilovs.andrejs.chess.domain.piece

import krilovs.andrejs.chess.domain.model.Board
import krilovs.andrejs.chess.domain.model.Color

class King(color: Color, square: Int) : Piece(color, square) {
  private val offsets = intArrayOf(-9, -8, -7, -1, 1, 7, 8, 9)

  override fun generateAvailableMoves(board: Board): List<Int> =
    stepTargets()
      .filter { board[it] == null || board[it]?.color != color }
      .toList()

  override fun generateAttacks(board: Board): List<Int> =
    stepTargets().toList()

  override fun copy(): Piece = King(color, square)

  private fun stepTargets(): Sequence<Int> =
    offsets.asSequence()
      .map { square + it }
      .filter { it.isValidStepFrom(square) }

  private fun Int.isValidStepFrom(from: Int) =
    isInsideBoard(this) &&
      kotlin.math.abs(file(from) - file(this)) <= 1 &&
      kotlin.math.abs(rank(from) - rank(this)) <= 1
}