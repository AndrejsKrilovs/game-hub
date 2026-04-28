package krilovs.andrejs.chess.piece

abstract class Piece(val color: Color, val square: Int) {
  val type: String get() = this::class.simpleName ?: "UNKNOWN"

  fun toDto() = mapOf(
    "type" to type,
    "color" to color.name,
    "coordinates" to mapOf(
      "file" to ('a' + (square % 8)).toString(),
      "rank" to (square / 8) + 1
    )
  )
}