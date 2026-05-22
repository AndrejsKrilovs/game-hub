package krilovs.andrejs.chess.domain.service

import krilovs.andrejs.chess.domain.model.Board
import krilovs.andrejs.chess.domain.model.Color
import krilovs.andrejs.chess.domain.model.GameState
import krilovs.andrejs.chess.domain.piece.Bishop
import krilovs.andrejs.chess.domain.piece.King
import krilovs.andrejs.chess.domain.piece.Knight
import krilovs.andrejs.chess.utils.BoardUtils

class GameStateService(private val moveSafety: MoveSafetyService) {
  fun getGameState(board: Board, currentTurn: Color): GameState {
    val kingSquare = BoardUtils.findKing(board, currentTurn)

    val hasMoves = board.getPieces()
      .filter { it.color == currentTurn }
      .flatMap { piece -> piece.generateAvailableMoves(board).map { to -> piece.square to to } }
      .any { (from, to) -> moveSafety.isSafeMove(board, from, to) }

    val inCheck = moveSafety.isSquareUnderAttack(board, kingSquare, currentTurn.opposite())
    val drawRule = isInsufficientMaterial(board)

    return when {
      !hasMoves && inCheck -> GameState.CHECKMATE
      !hasMoves -> GameState.STALEMATE
      inCheck -> GameState.CHECK
      drawRule -> GameState.DRAW
      else -> GameState.NORMAL
    }
  }

  private fun isInsufficientMaterial(board: Board): Boolean {
    val pieces = board.getPieces().toList()
    val nonKings = pieces.filterNot { it is King }

    if (nonKings.isEmpty()) {
      return true
    }
    if (nonKings.size == 1) {
      return nonKings.single() is Bishop || nonKings.single() is Knight
    }
    if (nonKings.size == 2 && nonKings.all { it is Bishop }) {
      val first = nonKings[0]
      val second = nonKings[1]
      return squareColor(first.square) == squareColor(second.square)
    }

    return false
  }

  private fun squareColor(square: Int): Int {
    val file = square % 8
    val rank = square / 8
    return (file + rank) % 2
  }
}