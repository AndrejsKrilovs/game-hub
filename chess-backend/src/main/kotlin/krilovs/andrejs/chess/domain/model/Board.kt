package krilovs.andrejs.chess.domain.model

import krilovs.andrejs.chess.domain.piece.Piece

class Board {
  private val squares = arrayOfNulls<Piece>(64)

  private val cachedPieces = LinkedHashSet<Piece>(32)
  private var piecesDirty = true

  operator fun get(square: Int): Piece? = squares[square]

  operator fun set(square: Int, piece: Piece?) {
    if (squares[square] === piece) return
    squares[square] = piece
    piecesDirty = true
  }

  fun getPieces(): Set<Piece> {
    if (piecesDirty) {
      cachedPieces.clear()

      for (piece in squares) {
        if (piece != null) {
          cachedPieces.add(piece)
        }
      }

      piecesDirty = false
    }

    return cachedPieces
  }

  fun clear() {
    squares.fill(null)
    cachedPieces.clear()
    piecesDirty = false
  }
}