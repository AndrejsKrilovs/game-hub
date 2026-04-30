package krilovs.andrejs.chess.piece

import krilovs.andrejs.chess.game.Board

class King(color: Color, square: Int) : Piece(color, square) {

  private val offsets = intArrayOf(-9, -8, -7, -1, 1, 7, 8, 9)

  override fun generateAvailableMoves(board: Board): Set<Int> =
    offsets
      .asSequence()
      .map { square + it }
      .filter { to ->
        if (!isInsideBoard(to)) return@filter false

        val df = kotlin.math.abs(file(square) - file(to))
        val dr = kotlin.math.abs(rank(square) - rank(to))

        // защита от "перепрыгивания" через край
        if (df > 1 || dr > 1) return@filter false

        val target = board[to]
        target == null || target.color != color
      }
      .toSet()
}