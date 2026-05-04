package krilovs.andrejs.chess.domain.piece

import krilovs.andrejs.chess.domain.model.Color

class Rook(color: Color, square: Int) : SlidingPiece(color, square, intArrayOf(8, -8, 1, -1)) {
  override fun copy(): Piece = Rook(color, square)
}