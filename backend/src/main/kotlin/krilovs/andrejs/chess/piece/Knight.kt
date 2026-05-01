package krilovs.andrejs.chess.piece

import krilovs.andrejs.chess.game.BoardService

class Knight(color: Color, square: Int) : Piece(color, square) {
  private val offsets = intArrayOf(17, 15, 10, 6, -6, -10, -15, -17)

  override fun generateAvailableMoves(board: BoardService): Set<Int> =
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