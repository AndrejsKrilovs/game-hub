package krilovs.andrejs.chess.piece

class Knight(color: Color, square: Int) : Piece(color, square) {
  private val offsets = intArrayOf(17, 15, 10, 6, -6, -10, -15, -17)

  override fun generateAvailableMoves(): Set<Int> =
    offsets
      .map { square + it }
      .filter { to ->
        isInsideBoard(to) &&
          kotlin.math.abs(file(square) - file(to)) +
          kotlin.math.abs(rank(square) - rank(to)) == 3
      }
      .toSet()
}