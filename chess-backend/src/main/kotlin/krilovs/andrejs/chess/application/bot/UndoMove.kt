package krilovs.andrejs.chess.application.bot

import krilovs.andrejs.chess.domain.model.Color
import krilovs.andrejs.chess.domain.piece.Piece

data class UndoMove(
  val from: Int,
  val to: Int,
  val movedPiece: Piece,
  val capturedPiece: Piece?,
  val previousCastling: String,
  val previousTurn: Color
)