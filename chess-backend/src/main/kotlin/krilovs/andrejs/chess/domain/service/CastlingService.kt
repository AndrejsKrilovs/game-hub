package krilovs.andrejs.chess.domain.service

import krilovs.andrejs.chess.domain.model.Board
import krilovs.andrejs.chess.domain.model.CastlingType
import krilovs.andrejs.chess.domain.model.Color
import krilovs.andrejs.chess.domain.piece.King
import krilovs.andrejs.chess.domain.piece.Piece

class CastlingService(private val moveSafety: MoveSafetyService) {
  fun getCastlingMoves(board: Board, king: King, castling: String): Set<Int> {
    val isWhite = king.color == Color.WHITE
    val enemy = king.color.opposite()

    return buildSet {
      castling.forEach { c ->
        if (c.isUpperCase() != isWhite) return@forEach

        when (c.uppercaseChar()) {
          'K' -> {
            val between = if (isWhite) listOf(5, 6) else listOf(61, 62)

            if (between.all { board[it] == null } &&
              !moveSafety.isSquareUnderAttack(board, king.square, enemy) &&
              !moveSafety.isSquareUnderAttack(board, between[0], enemy) &&
              !moveSafety.isSquareUnderAttack(board, between[1], enemy)
            ) {
              add(if (isWhite) 6 else 62)
            }
          }
          'Q' -> {
            val between = if (isWhite) listOf(1, 2, 3) else listOf(57, 58, 59)

            if (between.all { board[it] == null } &&
              !moveSafety.isSquareUnderAttack(board, king.square, enemy) &&
              !moveSafety.isSquareUnderAttack(board, between[1], enemy)
            ) {
              add(if (isWhite) 2 else 58)
            }
          }
        }
      }
    }
  }

  fun isCastling(piece: Piece, from: Int, to: Int): Boolean =
    piece is King && kotlin.math.abs(to - from) == 2

  fun getCastlingType(from: Int, to: Int): CastlingType =
    if (to - from == 2) CastlingType.SHORT else CastlingType.LONG

  fun getCastlingRightsToRemove(piece: Piece, to: Int, captured: Piece?): Set<Char> =
    buildSet {
      val rookRights = mapOf(0 to 'Q', 7 to 'K', 56 to 'q', 63 to 'k')

      if (piece is King) addAll(if (piece.color == Color.WHITE) "KQ".toList() else "kq".toList())
      if (piece is krilovs.andrejs.chess.domain.piece.Rook) rookRights[piece.square]?.let(::add)
      if (captured is krilovs.andrejs.chess.domain.piece.Rook) rookRights[to]?.let(::add)
    }
}