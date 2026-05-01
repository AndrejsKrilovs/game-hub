package krilovs.andrejs.chess.piece

import krilovs.andrejs.chess.game.BoardService

class King(color: Color, square: Int) : Piece(color, square) {
  private val offsets = intArrayOf(-9, -8, -7, -1, 1, 7, 8, 9)

  override fun generateAvailableMoves(board: BoardService): Set<Int> =
    stepTargets().filter { board[it] == null || board[it]?.color != color }.toSet()

  override fun generateAttacks(board: BoardService): Set<Int> = stepTargets().toSet()

  private fun stepTargets(): Sequence<Int> =
    offsets.asSequence()
      .map { square + it }
      .filter { it.isValidStepFrom(square) }

  private fun Int.isValidStepFrom(from: Int) =
    isInsideBoard(this) &&
      kotlin.math.abs(file(from) - file(this)) <= 1 &&
      kotlin.math.abs(rank(from) - rank(this)) <= 1
}