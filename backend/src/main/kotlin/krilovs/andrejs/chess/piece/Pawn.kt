package krilovs.andrejs.chess.piece

import krilovs.andrejs.chess.game.Board
import krilovs.andrejs.chess.game.Color
import krilovs.andrejs.chess.game.Move

class Pawn(color: Color, square: Int) : Piece(color, square) {

  override fun generateMoves(board: Board, moves: MutableList<Move>) {
    val dir = if (color == Color.WHITE) 8 else -8
    val startRank = if (color == Color.WHITE) 1 else 6

    val one = square + dir
    if (board.isInside(one) && board[one] == null) {
      moves += Move(square, one, this, null)

      val two = square + dir * 2
      if (board.rank(square) == startRank && board[two] == null) {
        moves += Move(square, two, this, null)
      }
    }

    listOf(dir + 1, dir - 1)
      .map { square + it }
      .filter { board.isInside(it) }
      .filter { kotlin.math.abs(board.file(square) - board.file(it)) == 1 }
      .mapNotNull { to ->
        board[to]?.takeIf { it.color != color }?.let { target ->
          Move(square, to, this, target)
        }
      }
      .forEach { moves += it }
  }
}