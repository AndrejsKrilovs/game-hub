package krilovs.andrejs.chess.piece

import krilovs.andrejs.chess.game.Move

abstract class Piece(val color: Color, val square: Int) {
  val type: String get() = this::class.simpleName ?: "UNKNOWN"

  fun generateMoves(): Set<Move> =
    generateAvailableMoves()
      .map { Move(square.toCord(), it.toCord(), this) }.toSet()

  fun toDto() = mapOf(
    "type" to type,
    "color" to color.name,
    "coordinates" to mapOf(
      "file" to ('a' + (square % 8)).toString(),
      "rank" to (square / 8) + 1
    )
  )

  protected fun file(sq: Int) = sq % 8
  protected fun rank(sq: Int) = sq / 8
  protected fun isInsideBoard(sq: Int) = sq in 0..63
  private fun Int.toCord() = "${'a' + (this % 8)}${(this / 8) + 1}"

  abstract fun generateAvailableMoves(): Set<Int>
}