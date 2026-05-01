package krilovs.andrejs.chess.game

import krilovs.andrejs.chess.piece.Color
import krilovs.andrejs.chess.piece.King
import org.springframework.stereotype.Component

@Component
class RuleService {
  fun isSafeMove(board: BoardService, from: Int, to: Int): Boolean {
    val piece = board[from] ?: return false
    val captured = board[to]

    board[from] = null
    board[to] = piece
    val oldSquare = piece.square
    piece.square = to

    val kingSquare =  board.pieces.first { it is King && it.color == piece.color }.square
    val inCheck = isSquareUnderAttack(board, kingSquare, piece.color.opposite())

    board[from] = piece
    board[to] = captured
    piece.square = oldSquare

    return !inCheck
  }

  fun isSquareUnderAttack(board: BoardService, square: Int, byColor: Color): Boolean =
    board.pieces
      .asSequence()
      .filter { it.color == byColor }
      .any { piece -> piece.generateAttacks(board).contains(square) }

  fun getGameState(board: BoardService, currentTurn: Color): GameState {
    val kingSquare = findKing(board, currentTurn)

    val hasMoves = board.pieces
      .asSequence()
      .filter { it.color == currentTurn }
      .flatMap { piece ->
        piece.generateAvailableMoves(board).asSequence().map { to -> piece.square to to }
      }
      .any { (from, to) -> isSafeMove(board, from, to) }

    val inCheck = isSquareUnderAttack(board, kingSquare, currentTurn.opposite())

    return when {
      inCheck && !hasMoves -> GameState.CHECKMATE
      !inCheck && !hasMoves -> GameState.STALEMATE
      inCheck -> GameState.CHECK
      else -> GameState.NORMAL
    }
  }

  private fun findKing(board: BoardService, color: Color): Int =
    board.pieces.first { it is King && it.color == color }.square
}