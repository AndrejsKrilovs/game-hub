package krilovs.andrejs.chess.game

import com.fasterxml.jackson.annotation.JsonInclude
import krilovs.andrejs.chess.piece.Color
import krilovs.andrejs.chess.piece.Piece

@JsonInclude(JsonInclude.Include.NON_NULL)
data class MoveDto(
  val from: String,
  val to: String,
  val piece: String,
  val color: Color,
  val promotionPiece: String? = null
)

data class Move(
  val from: String,
  val to: String,
  val piece: Piece,
  val promotionPiece: String? = null
) {
  fun toDto() = MoveDto(
    from = from,
    to = to,
    piece = piece.type,
    color = piece.color,
    promotionPiece = promotionPiece
  )
}