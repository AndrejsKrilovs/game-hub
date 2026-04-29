package krilovs.andrejs.chess.piece

class Rook(color: Color, square: Int) : Piece(color, square){
  override fun generateAvailableMoves(): Set<Int> {
    return emptySet()
  }
}