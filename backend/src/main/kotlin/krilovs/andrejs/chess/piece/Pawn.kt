package krilovs.andrejs.chess.piece

import krilovs.andrejs.chess.game.Board
import krilovs.andrejs.chess.game.Color
import krilovs.andrejs.chess.game.Move

class Pawn(color: Color, square: Int) : Piece(color, square) {

  override fun generateMoves(board: Board, moves: MutableList<Move>) {
    val dir = if (color == Color.WHITE) 8 else -8
    val startRank = if (color == Color.WHITE) 1 else 6

    val one = square + dir
    if (board[one] == null && board.isInside(one)) {
      addPawnMove(board, moves, one)
      val two = square + dir * 2
      if (board.rank(square) == startRank && board[two] == null) {
        moves += Move(square, two, this, null)
      }
    }

    for (to in listOf(square + dir - 1, square + dir + 1)) {
      if (!board.isInside(to)) continue
      if (kotlin.math.abs(board.file(square) - board.file(to)) != 1) continue
      board[to]?.takeIf { it.color != color }?.let { addPawnMove(board, moves, to) }
    }
  }

  private fun addPawnMove(board: Board, moves: MutableList<Move>, to: Int) {
    val target = board[to]
    val promotions = if (board.rank(to) == 0 || board.rank(to) == 7) PROMOTIONS else null
    promotions?.forEach { promo ->
      moves += Move(square, to, this, target, promotion = promo)
    } ?: moves.add(Move(square, to, this, target))
  }

  companion object {
    private val PROMOTIONS = charArrayOf('q', 'r', 'b', 'n')
  }
}