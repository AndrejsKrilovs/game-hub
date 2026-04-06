package krilovs.andrejs.chess.piece

import krilovs.andrejs.chess.game.Board
import krilovs.andrejs.chess.game.Color
import krilovs.andrejs.chess.game.Move

abstract class SlidingPiece(
  color: Color,
  square: Int,
  private val directions: IntArray
) : Piece(color, square) {

  override fun generateMoves(board: Board, moves: MutableList<Move>) {
    directions.forEach { dir ->
      var from = square

      while (true) {
        val to = from + dir
        if (!board.isInside(to)) break
        if (kotlin.math.abs(board.file(to) - board.file(from)) > 1) break

        val target = board[to]
        when {
          target == null -> moves += Move(square, to, this, null)
          target.color != color -> {
            moves += Move(square, to, this, target)
            break
          }
          else -> break
        }

        from = to
      }
    }
  }
}