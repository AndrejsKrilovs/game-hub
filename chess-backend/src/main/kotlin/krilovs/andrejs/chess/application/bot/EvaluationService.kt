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
import org.springframework.stereotype.Component

@Component
class EvaluationService {

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

  private val bishopTable = intArrayOf(
    -20,-10,-10,-10,-10,-10,-10,-20,
    -10,  5,  0,  0,  0,  0,  5,-10,
    -10, 10, 10, 10, 10, 10, 10,-10,
    -10,  0, 10, 10, 10, 10,  0,-10,
    -10,  5,  5, 10, 10,  5,  5,-10,
    -10,  0,  5, 10, 10,  5,  0,-10,
    -10,  0,  0,  0,  0,  0,  0,-10,
    -20,-10,-10,-10,-10,-10,-10,-20
  )

  private val pawnTable = intArrayOf(
    0,  0,  0,  0,  0,  0,  0,  0,
    50, 50, 50, 50, 50, 50, 50, 50,
    10, 10, 20, 30, 30, 20, 10, 10,
    5,  5, 10, 25, 25, 10,  5,  5,
    0,  0,  0, 20, 20,  0,  0,  0,
    5, -5,-10,  0,  0,-10, -5,  5,
    5, 10, 10,-20,-20, 10, 10,  5,
    0,  0,  0,  0,  0,  0,  0,  0
  )

  private val rookTable = intArrayOf(
    0,  0,  0,  5,  5,  0,  0,  0,
    -5,  0,  0,  0,  0,  0,  0, -5,
    -5,  0,  0,  0,  0,  0,  0, -5,
    -5,  0,  0,  0,  0,  0,  0, -5,
    -5,  0,  0,  0,  0,  0,  0, -5,
    -5,  0,  0,  0,  0,  0,  0, -5,
    5, 10, 10, 10, 10, 10, 10,  5,
    0,  0,  0,  0,  0,  0,  0,  0
  )

  private val queenTable = intArrayOf(
    -20,-10,-10, -5, -5,-10,-10,-20,
    -10,  0,  0,  0,  0,  0,  0,-10,
    -10,  0,  5,  5,  5,  5,  0,-10,
    -5,  0,  5,  5,  5,  5,  0, -5,
    0,  0,  5,  5,  5,  5,  0, -5,
    -10,  5,  5,  5,  5,  5,  0,-10,
    -10,  0,  5,  0,  0,  0,  0,-10,
    -20,-10,-10, -5, -5,-10,-10,-20
  )

  fun evaluate(board: Board, perspective: Color): Int {
    val pieces = board.getPieces().toList()

    val whiteAttacks = HashSet<Int>(64)
    val blackAttacks = HashSet<Int>(64)

    var whiteKingSquare = -1
    var blackKingSquare = -1

    for (piece in pieces) {
      if (piece is King) {
        if (piece.color == Color.WHITE) whiteKingSquare = piece.square
        else blackKingSquare = piece.square
      }

      val attacks = piece.generateAttacks(board)
      if (piece.color == Color.WHITE) whiteAttacks.addAll(attacks)
      else blackAttacks.addAll(attacks)
    }

    val context = EvalContext(
      pieces = pieces,
      whiteAttacks = whiteAttacks,
      blackAttacks = blackAttacks,
      whiteKingSquare = whiteKingSquare,
      blackKingSquare = blackKingSquare
    )

    val whiteScore = evaluateSide(board, Color.WHITE, context)
    val blackScore = evaluateSide(board, Color.BLACK, context)
    val score = whiteScore - blackScore

    return if (perspective == Color.WHITE) score else -score
  }

  private fun evaluateSide(board: Board, color: Color, context: EvalContext): Int {
    var score = 0

    var bishopCount = 0
    var knightCount = 0

    val ownPawnFiles = IntArray(8)
    val allPawnFiles = BooleanArray(8)

    for (piece in context.pieces) {
      if (piece is Pawn) {
        val file = piece.square % 8
        allPawnFiles[file] = true

        if (piece.color == color) {
          ownPawnFiles[file]++
        }
      }
    }

    for (piece in context.pieces) {
      if (piece.color != color) continue

      when (piece) {
        is Bishop -> bishopCount++
        is Knight -> knightCount++
      }

      score += pieceValue(piece)
      score += positionalBonus(piece)
      score += centerControlBonus(piece)
      score += developmentBonus(piece)
      score += pieceActivity(piece, board)
      score += pawnAdvancementBonus(piece)
      score += edgePenalty(piece)
      score += rookOpenFileBonus(piece, allPawnFiles)
      score += attackedPiecePenalty(piece, context)
      score += promotionThreatBonus(piece)
    }

    if (bishopCount >= 2) score += 40
    if (knightCount >= 2) score += 10

    score += doubledPawnPenalty(ownPawnFiles)
    score += isolatedPawnPenalty(ownPawnFiles)
    score += kingSafety(board, color, context)

    return score
  }

  private fun pieceValue(piece: Piece): Int =
    when (piece) {
      is Pawn -> 100
      is Knight -> 320
      is Bishop -> 330
      is Rook -> 500
      is Queen -> 900
      is King -> 20_000
      else -> 0
    }

  private fun positionalBonus(piece: Piece): Int {
    val index = normalizedIndex(piece)

    return when (piece) {
      is Pawn -> pawnTable[index]
      is Knight -> knightTable[index]
      is Bishop -> bishopTable[index]
      is Rook -> rookTable[index]
      is Queen -> queenTable[index]
      else -> 0
    }
  }

  private fun normalizedIndex(piece: Piece): Int = if (piece.color == Color.WHITE) piece.square else 63 - piece.square

  private fun developmentBonus(piece: Piece): Int {
    val square = piece.square

    return when (piece) {
      is Knight -> when (piece.color) {
        Color.WHITE -> if (square == 1 || square == 6) -25 else 0
        Color.BLACK -> if (square == 57 || square == 62) -25 else 0
      }
      is Bishop -> when (piece.color) {
        Color.WHITE -> if (square == 2 || square == 5) -20 else 0
        Color.BLACK -> if (square == 58 || square == 61) -20 else 0
      }
      is Queen -> {
        val rank = square / 8
        val earlyQueenMove =
          (piece.color == Color.WHITE && rank <= 2) ||
            (piece.color == Color.BLACK && rank >= 5)

        if (earlyQueenMove) -15 else 0
      }
      else -> 0
    }
  }

  private fun pieceActivity(piece: Piece, board: Board): Int {
    val moves = when (piece) {
      is Pawn, is King -> return 0
      is Queen -> piece.generateAvailableMoves(board).size.coerceAtMost(18)
      is Rook -> piece.generateAvailableMoves(board).size.coerceAtMost(14)
      is Bishop -> piece.generateAvailableMoves(board).size.coerceAtMost(13)
      is Knight -> piece.generateAvailableMoves(board).size
      else -> 0
    }

    return when (piece) {
      is Knight -> moves * 2
      is Bishop -> moves * 3
      is Rook -> moves * 2
      is Queen -> moves
      else -> 0
    }
  }

  private fun centerControlBonus(piece: Piece): Int {
    val file = piece.square % 8
    val rank = piece.square / 8

    val central = file in 2..5 && rank in 2..5
    val veryCentral = file in 3..4 && rank in 3..4

    return when {
      veryCentral && piece is Pawn -> 20
      veryCentral -> 15
      central -> 8
      else -> 0
    }
  }

  private fun pawnAdvancementBonus(piece: Piece): Int {
    if (piece !is Pawn) return 0
    val rank = piece.square / 8
    val advancement = when (piece.color) {
      Color.WHITE -> rank
      Color.BLACK -> 7 - rank
    }

    return when {
      advancement >= 6 -> 80
      advancement == 5 -> 45
      advancement == 4 -> 20
      else -> advancement * 4
    }
  }

  private fun edgePenalty(piece: Piece): Int {
    if (piece is King || piece is Pawn || piece is Rook) return 0

    val file = piece.square % 8
    val rank = piece.square / 8
    return if (file == 0 || file == 7 || rank == 0 || rank == 7) -8 else 0
  }

  private fun rookOpenFileBonus(piece: Piece, allPawnFiles: BooleanArray): Int {
    if (piece !is Rook) return 0

    val file = piece.square % 8
    return if (!allPawnFiles[file]) 35 else 0
  }

  private fun attackedPiecePenalty(piece: Piece, context: EvalContext): Int {
    if (piece is King) return 0

    val enemyAttacks = if (piece.color == Color.WHITE) {
      context.blackAttacks
    } else {
      context.whiteAttacks
    }

    val ownAttacks = if (piece.color == Color.WHITE) {
      context.whiteAttacks
    } else {
      context.blackAttacks
    }

    if (piece.square !in enemyAttacks) return 0

    val defended = piece.square in ownAttacks
    return if (defended) {
      -pieceValue(piece) / 20
    } else {
      -pieceValue(piece) / 4
    }
  }

  private fun doubledPawnPenalty(pawnFiles: IntArray): Int {
    var penalty = 0

    for (count in pawnFiles) {
      if (count > 1) {
        penalty -= (count - 1) * 20
      }
    }

    return penalty
  }

  private fun isolatedPawnPenalty(pawnFiles: IntArray): Int {
    var penalty = 0

    for (file in 0..7) {
      val count = pawnFiles[file]
      if (count == 0) continue

      val hasLeftSupport = file > 0 && pawnFiles[file - 1] > 0
      val hasRightSupport = file < 7 && pawnFiles[file + 1] > 0

      if (!hasLeftSupport && !hasRightSupport) {
        penalty -= count * 15
      }
    }

    return penalty
  }

  private fun kingSafety(board: Board, color: Color, context: EvalContext): Int {
    val kingSquare = when (color) {
      Color.WHITE -> context.whiteKingSquare
      Color.BLACK -> context.blackKingSquare
    }

    if (kingSquare !in 0..63) return 0

    val file = kingSquare % 8
    var score = kingPawnShield(board, kingSquare, color)
    if (file == 6 || file == 2) {
      score += 45
    }
    if (file in 3..4) {
      score -= 35
    }

    val enemyAttacks = if (color == Color.WHITE) {
      context.blackAttacks
    }
    else {
      context.whiteAttacks
    }

    var danger = 0
    for (square in kingNeighborhood(kingSquare)) {
      if (square in enemyAttacks) {
        danger += 8
      }
    }

    return score - danger
  }

  private fun kingNeighborhood(square: Int): List<Int> {
    val file = square % 8
    val rank = square / 8
    val result = ArrayList<Int>(8)

    for (df in -1..1) {
      for (dr in -1..1) {
        if (df == 0 && dr == 0) continue

        val f = file + df
        val r = rank + dr
        if (f in 0..7 && r in 0..7) {
          result.add(r * 8 + f)
        }
      }
    }

    return result
  }

  private fun kingPawnShield(board: Board, kingSquare: Int, color: Color): Int {
    val direction = if (color == Color.WHITE) 1 else -1

    var score = 0

    val square1 = kingSquare + direction * 8
    val square2 = kingSquare + direction * 8 - 1
    val square3 = kingSquare + direction * 8 + 1

    if (isFriendlyPawn(board, square1, color)) score += 15
    if (isFriendlyPawn(board, square2, color)) score += 15
    if (isFriendlyPawn(board, square3, color)) score += 15

    return score
  }

  private fun isFriendlyPawn(board: Board, square: Int, color: Color): Boolean {
    if (square !in 0..63) return false

    val piece = board[square]
    return piece is Pawn && piece.color == color
  }

  private fun promotionThreatBonus(piece: Piece): Int {
    if (piece !is Pawn) return 0

    val rank = piece.square / 8
    val distanceToPromotion = when (piece.color) {
      Color.WHITE -> 7 - rank
      Color.BLACK -> rank
    }

    return when (distanceToPromotion) {
      0 -> 800
      1 -> 300
      2 -> 140
      3 -> 60
      else -> 0
    }
  }
}

private data class EvalContext(
  val pieces: List<Piece>,
  val whiteAttacks: Set<Int>,
  val blackAttacks: Set<Int>,
  val whiteKingSquare: Int,
  val blackKingSquare: Int
)