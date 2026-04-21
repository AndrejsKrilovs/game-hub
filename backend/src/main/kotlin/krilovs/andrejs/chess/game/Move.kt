package krilovs.andrejs.chess.game

import krilovs.andrejs.chess.piece.Piece

data class Move(
  val from: Int,
  val to: Int,
  val piece: Piece,
  val captured: Piece?,
  val isCastling: Boolean = false,
  val promotion: Char? = null
)