package krilovs.andrejs.chess.game

import krilovs.andrejs.chess.piece.King
import krilovs.andrejs.chess.piece.Pawn
import krilovs.andrejs.chess.piece.Piece

class AttackService(private val board: Board) {
  fun isSquareUnderAttack(square: Int, byColor: Color): Boolean =
    board.pieces.any { piece -> piece.color == byColor && pieceAttacksSquare(piece, square) }

  private fun pieceAttacksSquare(piece: Piece, target: Int): Boolean =
    buildList {
      when (piece) {
        is Pawn -> generatePawnAttacks(piece, this)
        is King -> generateKingAttacks(piece, this)
        else -> piece.generateMoves(board, this)
      }
    }.any { it.to == target }

  private fun generatePawnAttacks(pawn: Pawn, moves: MutableList<Move>) {
    val dir = if (pawn.color == Color.WHITE) 8 else -8

    listOf(dir + 1, dir - 1)
      .map { pawn.square + it }
      .filter { board.isInside(it) }
      .filter { kotlin.math.abs(board.file(pawn.square) - board.file(it)) == 1 }
      .forEach { moves += Move(pawn.square, it, pawn, null) }
  }

  private fun generateKingAttacks(king: King, moves: MutableList<Move>) {
    val offsets = listOf(8, -8, 1, -1, 9, -9, 7, -7)

    offsets
      .map { king.square + it }
      .filter { board.isInside(it) }
      .filter { kotlin.math.abs(board.file(king.square) - board.file(it)) <= 1 }
      .forEach { moves += Move(king.square, it, king, null) }
  }
}