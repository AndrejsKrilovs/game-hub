package krilovs.andrejs.chess.game

import krilovs.andrejs.chess.piece.Piece

class Move(val from: String, val to: String, val piece: Piece) {
  fun toDto() = mapOf(
    "from" to from,
    "to" to to,
    "piece" to piece.type,
    "color" to piece.color
  )
}