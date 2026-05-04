package krilovs.andrejs.chess.domain.model

import krilovs.andrejs.chess.domain.piece.Piece

class Board {
  private val squares = Array<Piece?>(64) { null }

  operator fun get(square: Int): Piece? = squares[square]
  operator fun set(square: Int, piece: Piece?) { squares[square] = piece }

  fun getPieces(): Set<Piece> = squares.filterNotNull().toSet()
  fun clear() { squares.indices.forEach { squares[it] = null }}
  fun copy(): Board {
    val newBoard = Board()
    for (i in 0 .. 63) {
      newBoard[i] = this[i]?.copy()
    }
    return newBoard
  }
}