package krilovs.andrejs.chess.domain.piece

import krilovs.andrejs.chess.domain.model.Board
import krilovs.andrejs.chess.domain.model.Color

abstract class Piece(val color: Color, var square: Int) {
  val type: String get() = this::class.simpleName ?: "UNKNOWN"

  fun toDto() = mapOf(
    "type" to type,
    "color" to color.name,
    "coordinates" to "${'a' + (square % 8)}${(square / 8) + 1}"
  )

  protected fun file(sq: Int) = sq % 8
  protected fun rank(sq: Int) = sq / 8
  protected fun isInsideBoard(sq: Int) = sq in 0..63

  abstract fun copy(): Piece
  abstract fun generateAvailableMoves(board: Board): Set<Int>
  open fun generateAttacks(board: Board): Set<Int> = generateAvailableMoves(board)
}