package krilovs.andrejs.chess.domain.piece

import krilovs.andrejs.chess.domain.model.Color

class Bishop(color: Color, square: Int) : SlidingPiece(color, square, intArrayOf(9, -9, 7, -7)) {
  override fun copy(): Piece = Bishop(color, square)
}