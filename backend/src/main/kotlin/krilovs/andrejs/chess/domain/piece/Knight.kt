package krilovs.andrejs.chess.domain.piece

import krilovs.andrejs.chess.domain.model.Board
import krilovs.andrejs.chess.domain.model.Color

class Knight(color: Color, square: Int) : Piece(color, square) {
  private val offsets = intArrayOf(17, 15, 10, 6, -6, -10, -15, -17)

  override fun copy(): Piece = Knight(color, square)
  override fun generateAvailableMoves(board: Board): Set<Int> =
    offsets
      .asSequence()
      .map { square + it }
      .filter {
        if (!isInsideBoard(it)) return@filter false

        val df = kotlin.math.abs(file(square) - file(it))
        val dr = kotlin.math.abs(rank(square) - rank(it))
        if (df + dr != 3) return@filter false

        board[it] == null || board[it]?.color != color
      }
      .toSet()
}