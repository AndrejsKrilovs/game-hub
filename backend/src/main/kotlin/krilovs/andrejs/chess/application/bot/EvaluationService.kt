package krilovs.andrejs.chess.application.bot

import krilovs.andrejs.chess.domain.model.Board
import krilovs.andrejs.chess.domain.model.Color
import krilovs.andrejs.chess.domain.piece.Bishop
import krilovs.andrejs.chess.domain.piece.King
import krilovs.andrejs.chess.domain.piece.Knight
import krilovs.andrejs.chess.domain.piece.Pawn
import krilovs.andrejs.chess.domain.piece.Piece
import krilovs.andrejs.chess.domain.piece.Queen
import krilovs.andrejs.chess.domain.piece.Rook
import krilovs.andrejs.chess.domain.service.RuleFacade
import krilovs.andrejs.chess.utils.BoardUtils
import org.springframework.stereotype.Component
import kotlin.collections.any

@Component
class EvaluationService(private val ruleFacade: RuleFacade) {
  private val values = mapOf(
    "Pawn" to 100,
    "Knight" to 320,
    "Bishop" to 330,
    "Rook" to 500,
    "Queen" to 900,
    "King" to 20_000
  )

  private val knightTable = intArrayOf(
    -50,-40,-30,-30,-30,-30,-40,-50,
    -40,-20,  0,  5,  5,  0,-20,-40,
    -30,  5, 10, 15, 15, 10,  5,-30,
    -30,  0, 15, 20, 20, 15,  0,-30,
    -30,  5, 15, 20, 20, 15,  5,-30,
    -30,  0, 10, 15, 15, 10,  0,-30,
    -40,-20,  0,  0,  0,  0,-20,-40,
    -50,-40,-30,-30,-30,-30,-40,-50
  )

  fun evaluate(board: Board, perspective: Color): Int {
    val whiteScore = evaluateSide(board, Color.WHITE)
    val blackScore = evaluateSide(board, Color.BLACK)
    val score = whiteScore - blackScore
    return if (perspective == Color.WHITE) score else -score
  }

  private fun evaluateSide(board: Board, color: Color): Int {
    var score = 0
    val pieces = board.getPieces().filter { it.color == color }

    for (piece in pieces) {
      score += pieceValue(piece)
      score += positionalBonus(piece)
      score += pieceActivity(piece, board)
      score += centerControlBonus(piece)
      score += pawnAdvancementBonus(piece)
      score += edgePenalty(piece)
      score += rookOpenFileBonus(piece, board)
      score += hangingPiecePenalty(piece, board)
      score += promotionThreatBonus(piece)
    }

    score += mobilityScore(board, color)
    score += bishopPairBonus(board, color)
    score += doubledPawnPenalty(board, color)
    score += isolatedPawnPenalty(board, color)
    score += kingSafety(board, color)
    return score
  }

  private fun pieceValue(piece: Piece): Int = values[piece.type] ?: 0

  private fun positionalBonus(piece: Piece): Int {
    val index = normalizedIndex(piece)
    return when (piece) {
      is Knight -> knightTable[index]
      else -> 0
    }
  }

  private fun normalizedIndex(piece: Piece): Int =
    if (piece.color == Color.WHITE) piece.square else 63 - piece.square

  private fun pieceActivity(piece: Piece, board: Board): Int {
    val moves = piece.generateAvailableMoves(board).size
    return when (piece) {
      is Knight, is Bishop -> moves * 4
      is Rook -> moves * 2
      is Queen -> moves
      else -> 0
    }
  }

  private fun centerControlBonus(piece: Piece): Int {
    val file = piece.square % 8
    val rank = piece.square / 8
    return if (file in 2..5 && rank in 2..5) 20 else 0
  }

  private fun pawnAdvancementBonus(piece: Piece): Int {
    if (piece !is Pawn) return 0
    val rank = piece.square / 8
    val advancement = when (piece.color) {
      Color.WHITE -> rank
      Color.BLACK -> 7 - rank
    }

    return advancement * 10
  }

  private fun edgePenalty(piece: Piece): Int {
    if (piece is King || piece is Pawn) {
      return 0
    }

    val file = piece.square % 8
    val rank = piece.square / 8
    return if (file == 0 || file == 7 || rank == 0 || rank == 7) -10 else 0
  }

  private fun rookOpenFileBonus(piece: Piece, board: Board): Int {
    if (piece !is Rook) return 0
    val file = piece.square % 8
    val pawnOnFile = board.getPieces().any { it is Pawn && (it.square % 8) == file }
    return if (!pawnOnFile) 35 else 0
  }

  private fun hangingPiecePenalty(piece: Piece, board: Board): Int {
    val attacked = ruleFacade.moveSafety.isSquareUnderAttack(board, piece.square, piece.color.opposite())
    val defended = defendedByFriendlyPiece(board, piece)
    return if (attacked && !defended) -pieceValue(piece) / 4 else 0
  }
  private fun defendedByFriendlyPiece(board: Board, targetPiece: Piece): Boolean =
    board.getPieces()
      .asSequence()
      .filter { it.color == targetPiece.color }
      .filterNot { it.square == targetPiece.square }
      .any { piece -> piece.generateAvailableMoves(board).any { it == targetPiece.square } }

  private fun bishopPairBonus(board: Board, color: Color): Int {
    val bishops = board.getPieces().count { it.color == color && it is Bishop }
    return if (bishops >= 2) 40 else 0
  }

  private fun doubledPawnPenalty(board: Board, color: Color): Int {
    val pawns = board.getPieces().filter { it.color == color && it is Pawn }
    val files = pawns.groupBy { it.square % 8 }
    return files.values.sumOf { if (it.size > 1) (it.size - 1) * -20 else 0 }
  }

  private fun isolatedPawnPenalty(board: Board, color: Color): Int {
    val pawns = board.getPieces().filter { it.color == color && it is Pawn }
    val filesWithPawns = pawns.map { it.square % 8 }
    var penalty = 0

    for (pawn in pawns) {
      val file = pawn.square % 8
      val hasLeftSupport = filesWithPawns.contains(file - 1)
      val hasRightSupport = filesWithPawns.contains(file + 1)
      if (!hasLeftSupport && !hasRightSupport) {
        penalty -= 15
      }
    }

    return penalty
  }

  private fun mobilityScore(board: Board, color: Color): Int = countMoves(board, color) * 3

  private fun countMoves(board: Board, color: Color): Int {
    return board.getPieces()
      .filter { it.color == color }
      .sumOf { it.generateAvailableMoves(board).size }
  }

  private fun kingSafety(board: Board, color: Color): Int {
    val kingSquare = BoardUtils.findKing(board, color)
    val file = kingSquare % 8
    var score = kingPawnShield(board, kingSquare, color)

    if (file == 6 || file == 2) {
      score += 40
    }
    if (file in 3..4) {
      score -= 30
    }

    return score
  }

  private fun kingPawnShield(board: Board, kingSquare: Int, color: Color): Int {
    val direction = if (color == Color.WHITE) 1 else -1
    val shieldSquares = listOf(
      kingSquare + direction * 8,
      kingSquare + direction * 8 - 1,
      kingSquare + direction * 8 + 1
    )

    var score = 0

    for (square in shieldSquares) {
      val piece = board[square]
      if (piece is Pawn && piece.color == color) {
        score += 15
      }
    }

    return score
  }

  private fun promotionThreatBonus(piece: Piece): Int {
    if (piece !is Pawn) return 0
    val rank = piece.square / 8
    val distanceToPromotion = when (piece.color) {
      Color.WHITE -> 7 - rank
      Color.BLACK -> rank
    }
    return when (distanceToPromotion) {
      1 -> 250
      2 -> 120
      3 -> 60
      else -> 0
    }
  }
}