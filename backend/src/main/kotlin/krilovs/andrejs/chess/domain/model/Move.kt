package krilovs.andrejs.chess.domain.model

import com.fasterxml.jackson.annotation.JsonInclude
import krilovs.andrejs.chess.domain.piece.Piece

@JsonInclude(JsonInclude.Include.NON_NULL)
data class MoveDto(
  val from: String,
  val to: String,
  val piece: String,
  val color: Color,
  val promotionPiece: String? = null,
  val castlingType: CastlingType? = null
)

data class Move(
  val from: String,
  val to: String,
  val piece: Piece,
  val promotionPiece: String? = null,
  val castlingType: CastlingType? = null
) {
  fun toDto() = MoveDto(
    from = from,
    to = to,
    piece = piece.type,
    color = piece.color,
    promotionPiece = promotionPiece,
    castlingType = castlingType
  )
}