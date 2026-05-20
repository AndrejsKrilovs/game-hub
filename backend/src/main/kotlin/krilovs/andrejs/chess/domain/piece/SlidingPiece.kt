package krilovs.andrejs.chess.domain.piece

import krilovs.andrejs.chess.domain.model.Board
import krilovs.andrejs.chess.domain.model.Color

abstract class SlidingPiece(color: Color, square: Int, private val offsets: IntArray) : Piece(color, square) {

  override fun generateAvailableMoves(board: Board): List<Int> =
    offsets
      .asSequence()
      .flatMap { offset ->

        val ray = generateSequence(square) { current ->
          val next = current + offset
          if (!isValidStep(current, next, offset)) return@generateSequence null
          next
        }
          .drop(1)
          .toList()

        val emptySquares = ray.takeWhile { board[it] == null }

        val captureSquare = ray
          .dropWhile { board[it] == null }
          .firstOrNull()
          ?.let { pos ->
            val target = board[pos]
            if (target != null && target.color != color) pos else null
          }

        (emptySquares + listOfNotNull(captureSquare)).asSequence()
      }
      .toList()

  private fun isValidStep(from: Int, to: Int, offset: Int): Boolean {
    if (!isInsideBoard(to)) return false

    val df = kotlin.math.abs(file(from) - file(to))
    val dr = kotlin.math.abs(rank(from) - rank(to))

    return when (offset) {
      1, -1 -> df == 1 && dr == 0      // горизонталь
      8, -8 -> df == 0 && dr == 1      // вертикаль
      9, -9 -> df == 1 && dr == 1      // диагональ ↘↖
      7, -7 -> df == 1 && dr == 1      // диагональ ↙↗
      else -> false
    }
  }
}