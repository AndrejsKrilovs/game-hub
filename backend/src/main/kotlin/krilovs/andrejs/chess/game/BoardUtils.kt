package krilovs.andrejs.chess.game

object BoardUtils {
  fun toCord(number: Int): String = "${'a' + (number % 8)}${(number / 8) + 1}"
  fun toSquare(cell: String): Int = (cell[1].digitToInt() - 1) * 8 + (cell[0] - 'a')
}