package krilovs.andrejs.chess.game

import krilovs.andrejs.chess.piece.Piece

data class Move(
  val from: String,
  val to: String,
  val piece: Piece
)