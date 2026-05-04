package krilovs.andrejs.chess.game

import krilovs.andrejs.chess.piece.Bishop
import krilovs.andrejs.chess.piece.Color
import krilovs.andrejs.chess.piece.King
import krilovs.andrejs.chess.piece.Knight
import krilovs.andrejs.chess.piece.Pawn
import krilovs.andrejs.chess.piece.Piece
import krilovs.andrejs.chess.piece.Queen
import krilovs.andrejs.chess.piece.Rook
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