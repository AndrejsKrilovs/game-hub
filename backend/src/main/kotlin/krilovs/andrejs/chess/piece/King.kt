package krilovs.andrejs.chess.piece

import krilovs.andrejs.chess.game.BoardService

class King(color: Color, square: Int) : Piece(color, square) {
  private val offsets = intArrayOf(-9, -8, -7, -1, 1, 7, 8, 9)

  override fun generateAvailableMoves(board: BoardService): Set<Int> =
    stepTargets().toMutableSet()
      .apply { addAll(generateCastleMoves(board)) }
      .filter { board[it] == null || board[it]?.color != color }
      .toSet()

  override fun generateAttacks(board: BoardService): Set<Int> = stepTargets().toSet()

  private fun stepTargets(): Sequence<Int> =
    offsets.asSequence()
      .map { square + it }
      .filter { it.isValidStepFrom(square) }

  private fun generateCastleMoves(board: BoardService): Set<Int> = buildSet {
    val isWhite = color == Color.WHITE

    board.castlingOption.forEach { c ->
      if (c.isUpperCase() != isWhite) return@forEach

      when (c.uppercaseChar()) {
        'K' -> add(if (isWhite) 6 else 62)
        'Q' -> add(if (isWhite) 2 else 58)
      }
    }
  }

  private fun Int.isValidStepFrom(from: Int) =
    isInsideBoard(this) &&
      kotlin.math.abs(file(from) - file(this)) <= 1 &&
      kotlin.math.abs(rank(from) - rank(this)) <= 1
}