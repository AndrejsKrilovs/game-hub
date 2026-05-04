package krilovs.andrejs.chess.application

import krilovs.andrejs.chess.domain.model.Color
import krilovs.andrejs.chess.domain.piece.Bishop
import krilovs.andrejs.chess.domain.piece.King
import krilovs.andrejs.chess.domain.piece.Knight
import krilovs.andrejs.chess.domain.piece.Pawn
import krilovs.andrejs.chess.domain.piece.Piece
import krilovs.andrejs.chess.domain.piece.Queen
import krilovs.andrejs.chess.domain.piece.Rook
import org.springframework.stereotype.Component

@Component
class PieceFactory {
  fun create(type: Char, color: Color, square: Int): Piece =
    when (type) {
      'k' -> King(color, square)
      'p' -> Pawn(color, square)
      'r' -> Rook(color, square)
      'q' -> Queen(color, square)
      'n' -> Knight(color, square)
      'b' -> Bishop(color, square)
      else -> error("Некорректная фигура: $type")
    }
}