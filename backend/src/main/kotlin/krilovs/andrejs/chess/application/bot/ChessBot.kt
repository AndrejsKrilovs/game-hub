package krilovs.andrejs.chess.application.bot

import krilovs.andrejs.chess.application.GameService
import krilovs.andrejs.chess.domain.model.Color
import krilovs.andrejs.chess.domain.model.Move
import krilovs.andrejs.chess.domain.piece.Bishop
import krilovs.andrejs.chess.domain.piece.King
import krilovs.andrejs.chess.domain.piece.Knight
import krilovs.andrejs.chess.domain.piece.Pawn
import krilovs.andrejs.chess.domain.piece.Piece
import krilovs.andrejs.chess.domain.piece.Queen
import krilovs.andrejs.chess.domain.piece.Rook
import krilovs.andrejs.chess.utils.BoardUtils
import org.springframework.stereotype.Component
import kotlin.math.abs

@Component
class ChessBot(
  private val game: GameService,
  private val evaluation: EvaluationService
) {

  private val table = HashMap<Long, TTEntry>(200_000)

  fun findBestMove(): Move? {
    table.clear()

    val botColor = game.currentTurn
    val result = searchMoves(
      depth = SEARCH_DEPTH,
      alpha = -INF,
      beta = INF,
      botColor = botColor,
      ttMove = null
    )

    val move = result.move ?: return null
    val piece = game.getPiece(move.from) ?: return null
    return Move(
      from = BoardUtils.toCord(move.from),
      to = BoardUtils.toCord(move.to),
      piece = piece
    )
  }

  private fun alphaBeta(depth: Int, alpha: Int, beta: Int, botColor: Color): Int {
    if (depth == 0) return evaluatePosition(botColor)

    val key = positionKey()
    var a = alpha
    var b = beta
    val originalAlpha = alpha

    val cached = table[key]
    if (cached != null && cached.depth >= depth) {
      when (cached.flag) {
        TTFlag.EXACT -> return cached.score
        TTFlag.LOWER_BOUND -> a = maxOf(a, cached.score)
        TTFlag.UPPER_BOUND -> b = minOf(b, cached.score)
      }

      if (a >= b) return cached.score
    }

    val result = searchMoves(
      depth = depth,
      alpha = a,
      beta = b,
      botColor = botColor,
      ttMove = cached?.bestMove
    )

    val score = result.score
    val flag = when {
      score <= originalAlpha -> TTFlag.UPPER_BOUND
      score >= beta -> TTFlag.LOWER_BOUND
      else -> TTFlag.EXACT
    }

    table[key] = TTEntry(
      depth = depth,
      score = score,
      flag = flag,
      bestMove = result.move
    )

    return score
  }

  private fun searchMoves(depth: Int, alpha: Int, beta: Int, botColor: Color, ttMove: IntMove?): SearchResult {
    val moves = orderedMoves(ttMove)
    if (moves.isEmpty()) {
      return SearchResult(
        move = null,
        score = evaluatePosition(botColor)
      )
    }

    val maximizing = game.currentTurn == botColor
    var a = alpha
    var b = beta
    var bestMove: IntMove? = null
    var bestScore = if (maximizing) -INF else INF

    for (move in moves) {
      val undo = game.makeMoveInternal(move.from, move.to)

      val score = alphaBeta(
        depth = depth - 1,
        alpha = a,
        beta = b,
        botColor = botColor
      )

      game.undoMove(undo)

      val better = if (maximizing) {
        score > bestScore
      } else {
        score < bestScore
      }

      if (better) {
        bestScore = score
        bestMove = move
      }

      if (maximizing) {
        a = maxOf(a, bestScore)
      } else {
        b = minOf(b, bestScore)
      }

      if (a >= b) break
    }

    return SearchResult(
      move = bestMove,
      score = bestScore
    )
  }

  private fun orderedMoves(ttMove: IntMove?): List<IntMove> =
    game.getAllMovePairs()
      .map { (from, to) -> IntMove(from, to) }
      .sortedByDescending { move -> moveScore(move, ttMove) }

  private fun moveScore(move: IntMove, ttMove: IntMove?): Int {
    if (move == ttMove) return TT_MOVE_SCORE

    val attacker = game.getPiece(move.from) ?: return 0
    val victim = game.getPiece(move.to)

    if (victim != null) {
      return CAPTURE_SCORE + pieceValue(victim) * 10 - pieceValue(attacker)
    }

    return positionalMoveScore(move, attacker)
  }

  private fun positionalMoveScore(move: IntMove, piece: Piece): Int {
    val toFile = move.to % 8
    val toRank = move.to / 8
    val fromRank = move.from / 8

    var score = 0

    if (toFile in 2..5 && toRank in 2..5) score += 20
    if (toFile in 3..4 && toRank in 3..4) score += 15

    score += when (piece) {
      is Pawn -> {
        if (toRank in 3..4) 10 else 0
      }
      is Knight, is Bishop -> {
        if (move.from in START_MINOR_SQUARES) 25 else 0
      }
      is Rook -> {
        if (toRank == 1 || toRank == 6) 10 else 0
      }
      is Queen -> {
        val earlyQueenMove =
          (fromRank == 0 && toRank <= 2) ||
            (fromRank == 7 && toRank >= 5)
        if (earlyQueenMove) -20 else 0
      }
      is King -> {
        if (abs(move.to - move.from) == 2) 80 else 0
      }
      else -> 0
    }
    return score
  }

  private fun evaluatePosition(botColor: Color): Int = evaluation.evaluate(game.getBoard(), botColor)

  private fun positionKey(): Long {
    var hash = HASH_OFFSET

    for (square in 0..63) {
      val piece = game.getPiece(square) ?: continue

      hash = mix(hash, square)
      hash = mix(hash, pieceCode(piece))
      hash = mix(hash, piece.color.ordinal)
    }

    hash = mix(hash, game.currentTurn.ordinal)

    for (char in game.castlingOption) {
      hash = mix(hash, char.code)
    }

    return hash
  }

  private fun mix(hash: Long, value: Int): Long = (hash xor value.toLong()) * HASH_PRIME

  private fun pieceCode(piece: Piece): Int =
    when (piece) {
      is Pawn -> 1
      is Knight -> 2
      is Bishop -> 3
      is Rook -> 4
      is Queen -> 5
      is King -> 6
      else -> 0
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

  companion object {
    private const val SEARCH_DEPTH = 4
    private const val INF = Int.MAX_VALUE - 1

    private const val TT_MOVE_SCORE = 1_000_000
    private const val CAPTURE_SCORE = 100_000

    private const val HASH_OFFSET = 1469598103934665603L
    private const val HASH_PRIME = 1099511628211L

    private val START_MINOR_SQUARES = setOf(1, 2, 5, 6, 57, 58, 61, 62)
  }
}

private data class IntMove(
  val from: Int,
  val to: Int
)

private data class SearchResult(
  val move: IntMove?,
  val score: Int
)

private data class TTEntry(
  val depth: Int,
  val score: Int,
  val flag: TTFlag,
  val bestMove: IntMove?
)

private enum class TTFlag {
  EXACT,
  LOWER_BOUND,
  UPPER_BOUND
}