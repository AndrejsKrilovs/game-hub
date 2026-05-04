package krilovs.andrejs.chess.domain.piece

import krilovs.andrejs.chess.domain.model.Color

class Queen(color: Color, square: Int) : SlidingPiece(color, square, intArrayOf(8, -8, 1, -1, 9, -9, 7, -7)) {
  override fun copy(): Piece = Queen(color, square)
}