package krilovs.andrejs.chess.piece

import krilovs.andrejs.chess.game.Board

class Pawn(color: Color, square: Int) : Piece(color, square) {
  private val dir = if (color == Color.WHITE) 8 else -8
  private val startRank = if (color == Color.WHITE) 1 else 6
  private val offsets = intArrayOf(dir, dir - 1, dir + 1)

  override fun generateAvailableMoves(board: Board): Set<Int> =
    offsets
      .asSequence()
      .map { square + it }
      .filter { to ->
        if (!isInsideBoard(to)) return@filter false

        val df = kotlin.math.abs(file(square) - file(to))
        val target = board[to]

        when {
          // движение вперёд
          to == square + dir -> target == null

          // взятия
          df == 1 && (to == square + dir - 1 || to == square + dir + 1) -> target != null && target.color != color
          else -> false
        }
      }
      .toMutableSet()
      .apply {
        // двойной ход отдельно
        val one = square + dir
        val two = square + dir * 2

        if (square / 8 == startRank &&
          isInsideBoard(one) && board[one] == null &&
          isInsideBoard(two) && board[two] == null
        ) {
          add(two)
        }
      }
}