package krilovs.andrejs.chess.domain.service

import krilovs.andrejs.chess.domain.model.Board
import krilovs.andrejs.chess.domain.model.Color
import krilovs.andrejs.chess.domain.piece.King
import kotlin.math.abs

class MoveSafetyService {
  fun isSafeMove(board: Board, from: Int, to: Int): Boolean {
    val piece = board[from] ?: return false
    val captured = board[to]

    if (piece is King && abs(to - from) == 2) {
      if (isSquareUnderAttack(board, findKing(board, piece.color), piece.color.opposite())) return false

      val step = if (to > from) 1 else -1
      if (isSquareUnderAttack(board, from + step, piece.color.opposite())) return false
    }

    board[from] = null
    board[to] = piece
    val oldSquare = piece.square
    piece.square = to

    val inCheck = isSquareUnderAttack(board, findKing(board, piece.color), piece.color.opposite())

    board[from] = piece
    board[to] = captured
    piece.square = oldSquare

    return !inCheck
  }

  fun isSquareUnderAttack(board: Board, square: Int, byColor: Color): Boolean =
    board.getPieces()
      .filter { it.color == byColor }
      .any { it.generateAttacks(board).contains(square) }

  fun findKing(board: Board, color: Color): Int =
    board.getPieces().first { it is King && it.color == color }.square
}