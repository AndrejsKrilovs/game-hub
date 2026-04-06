package krilovs.andrejs.chess.piece

import krilovs.andrejs.chess.game.Board
import krilovs.andrejs.chess.game.Color
import krilovs.andrejs.chess.game.Move

class King(color: Color, square: Int) : Piece(color, square) {
  private val offsets = intArrayOf(8, -8, 1, -1, 9, -9, 7, -7)

  override fun generateMoves(board: Board, moves: MutableList<Move>) {
    generateNormalMoves(board, moves)
    generateCastling(board, moves)
  }

  private fun generateNormalMoves(board: Board, moves: MutableList<Move>) {
    offsets.forEach { offset ->
      val to = square + offset
      if (!board.isInside(to)) return@forEach
      if (kotlin.math.abs(board.file(square) - board.file(to)) > 1) return@forEach
      addMove(board, moves, to)
    }
  }

  private fun generateCastling(board: Board, moves: MutableList<Move>) {
    val data = board.castlingData(color)
    if (square != data.kingStart || data.kingMoved) return
    if (!data.rookHMoved && data.emptyShort.all { board[it] == null }) {
      moves += Move(from = square, to = square + 2, piece = this, captured = null, isCastling = true)
    }
    if (!data.rookAMoved && data.emptyLong.all { board[it] == null }) {
      moves += Move(from = square, to = square - 2, piece = this, captured = null, isCastling = true)
    }
  }
}