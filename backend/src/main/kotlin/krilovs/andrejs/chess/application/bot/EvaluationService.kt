package krilovs.andrejs.chess.application.bot

import krilovs.andrejs.chess.domain.model.Board
import krilovs.andrejs.chess.domain.model.Color
import krilovs.andrejs.chess.domain.piece.Piece
import krilovs.andrejs.chess.utils.BoardUtils
import org.springframework.stereotype.Component

@Component
class EvaluationService {
  private val values = mapOf(
    "Pawn" to 100,
    "Knight" to 300,
    "Bishop" to 400,
    "Rook" to 500,
    "Queen" to 900,
    "King" to 10_000
  )

  fun evaluate(board: Board, perspective: Color): Int {
    val whiteKing = BoardUtils.findKing(board, Color.WHITE)
    val blackKing = BoardUtils.findKing(board, Color.BLACK)

    val materialAndPosition = board.getPieces().sumOf { piece ->
      val base = values[piece.type] ?: 0
      val bonus = pieceBonus(piece, board)
      val total = base + bonus
      if (piece.color == perspective) total else -total
    }

    val mobility = mobilityScore(board, perspective)
    val kingSafety =
      kingSafetyBonus(whiteKing, Color.WHITE, perspective) +
        kingSafetyBonus(blackKing, Color.BLACK, perspective)

    return materialAndPosition + mobility + kingSafety
  }

  private fun pieceBonus(piece: Piece, board: Board): Int =
    pawnAdvancementBonus(piece) +
      centerControlBonus(piece) +
      activityBonus(piece, board) +
      edgePenalty(piece)

  private fun pawnAdvancementBonus(piece: Piece): Int {
    if (piece.type != "Pawn") return 0
    val rank = piece.square / 8
    val advancement = when (piece.color) {
      Color.WHITE -> rank
      Color.BLACK -> 7 - rank
    }

    return advancement * 12
  }

  private fun centerControlBonus(piece: Piece): Int {
    val file = piece.square % 8
    val rank = piece.square / 8
    return if (file in 2..5 && rank in 2..5) 20 else 0
  }

  private fun activityBonus(piece: Piece, board: Board): Int {
    val moves = piece.generateAvailableMoves(board).size

    return when (piece.type) {
      "Knight", "Bishop" -> moves * 4
      "Rook" -> moves * 2
      "Queen" -> moves
      else -> 0
    }
  }

  private fun edgePenalty(piece: Piece): Int {
    val file = piece.square % 8
    val rank = piece.square / 8
    val isEdge = file == 0 || file == 7 || rank == 0 || rank == 7
    return if (isEdge && piece.type != "King" && piece.type != "Pawn") -10 else 0
  }

  private fun kingSafetyBonus(square: Int, color: Color, perspective: Color): Int {
    val file = square % 8
    val bonus = when (file) {
      6, 2 -> 60   // рокировка — очень хорошо
      0, 7 -> 20
      else -> -40  // центр — опасно
    }

    return if (color == perspective) bonus else -bonus
  }

  private fun mobilityScore(board: Board, perspective: Color): Int {
    val myMoves = countMoves(board, perspective)
    val opponentMoves = countMoves(board, perspective.opposite())
    return (myMoves - opponentMoves) * 5
  }

  private fun countMoves(board: Board, color: Color): Int =
    board.getPieces()
      .filter { it.color == color }
      .sumOf { it.generateAvailableMoves(board).size }
}