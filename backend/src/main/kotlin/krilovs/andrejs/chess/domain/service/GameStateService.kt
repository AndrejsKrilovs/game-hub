package krilovs.andrejs.chess.domain.service

import krilovs.andrejs.chess.domain.model.Board
import krilovs.andrejs.chess.domain.model.Color
import krilovs.andrejs.chess.domain.model.GameState

class GameStateService(private val moveSafety: MoveSafetyService) {
  fun getGameState(board: Board, currentTurn: Color): GameState {
    val kingSquare = moveSafety.findKing(board, currentTurn)

    val hasMoves = board.getPieces()
      .filter { it.color == currentTurn }
      .flatMap { piece ->
        piece.generateAvailableMoves(board).map { to -> piece.square to to }
      }
      .any { (from, to) -> moveSafety.isSafeMove(board, from, to) }

    val inCheck = moveSafety.isSquareUnderAttack(board, kingSquare, currentTurn.opposite())

    return when {
      !hasMoves && inCheck -> GameState.CHECKMATE
      !hasMoves -> GameState.STALEMATE
      inCheck -> GameState.CHECK
      else -> GameState.NORMAL
    }
  }
}