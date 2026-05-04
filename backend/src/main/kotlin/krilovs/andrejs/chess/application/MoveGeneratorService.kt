package krilovs.andrejs.chess.application

import krilovs.andrejs.chess.domain.model.*
import krilovs.andrejs.chess.domain.piece.King
import krilovs.andrejs.chess.domain.piece.Piece
import krilovs.andrejs.chess.domain.service.RuleFacade
import org.springframework.stereotype.Component

@Component
class MoveGeneratorService(private val rules: RuleFacade) {
  fun generateLegalMoves(board: Board, piece: Piece, castlingOption: String): Set<Int> {
    val from = piece.square
    val moves = piece.generateAvailableMoves(board).toMutableSet()
    if (piece is King) {
      moves.addAll(rules.castling.getCastlingMoves(board, piece, castlingOption))
    }

    return moves.filter { to -> rules.moveSafety.isSafeMove(board, from, to) }.toSet()
  }

  fun getAllMoves(board: Board, color: Color, castlingOption: String): List<Pair<Int, Int>> {
    return board.getPieces()
      .filter { it.color == color }
      .flatMap { piece ->
        generateLegalMoves(board, piece, castlingOption).map { to -> piece.square to to }
      }
  }
}