package krilovs.andrejs.chess.utils

import krilovs.andrejs.chess.domain.model.Board
import krilovs.andrejs.chess.domain.model.Color
import krilovs.andrejs.chess.domain.piece.King

object BoardUtils {
  fun toCord(number: Int): String = "${'a' + (number % 8)}${(number / 8) + 1}"
  fun toSquare(cell: String): Int = (cell[1].digitToInt() - 1) * 8 + (cell[0] - 'a')
  fun findKing(board: Board, color: Color): Int = board.getPieces().first { it is King && it.color == color }.square
}