package krilovs.andrejs.chess.domain.service

import krilovs.andrejs.chess.domain.model.Color
import krilovs.andrejs.chess.domain.piece.Pawn
import krilovs.andrejs.chess.domain.piece.Piece

class PromotionService {
  fun isPromotion(piece: Piece, to: Int): Boolean =
    piece is Pawn && when (piece.color) {
      Color.WHITE -> to / 8 == 7
      Color.BLACK -> to / 8 == 0
    }
}