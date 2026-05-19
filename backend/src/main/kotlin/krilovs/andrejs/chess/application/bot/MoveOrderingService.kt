package krilovs.andrejs.chess.application.bot

import krilovs.andrejs.chess.application.GameService
import krilovs.andrejs.chess.domain.piece.Bishop
import krilovs.andrejs.chess.domain.piece.King
import krilovs.andrejs.chess.domain.piece.Knight
import krilovs.andrejs.chess.domain.piece.Pawn
import krilovs.andrejs.chess.domain.piece.Piece
import krilovs.andrejs.chess.domain.piece.Queen
import krilovs.andrejs.chess.domain.piece.Rook
import org.springframework.stereotype.Component
import kotlin.math.abs

@Component
class MoveOrderingService(private val game: GameService) {

  private val killers = Array(MAX_PLY) { arrayOfNulls<IntMove>(2) }
  private val history = Array(2) { Array(64) { IntArray(64) } }

  fun clear() {
    clearKillers()
    clearHistory()
  }

  fun orderedMoves(ttMove: IntMove?, ply: Int): List<IntMove> =
    game.getAllMovePairs()
      .map { (from, to) -> IntMove(from, to) }
      .sortedByDescending { move -> moveScore(move, ttMove, ply) }

  fun isCapture(move: IntMove): Boolean = game.getPiece(move.to) != null

  fun storeKiller(move: IntMove, ply: Int) {
    if (ply !in 0 until MAX_PLY) return
    if (killers[ply][0] == move) return

    killers[ply][1] = killers[ply][0]
    killers[ply][0] = move
  }

  fun addHistoryBonus(move: IntMove, depth: Int) {
    val piece = game.getPiece(move.from) ?: return
    val bonus = depth * depth

    val colorIndex = piece.color.ordinal
    val current = history[colorIndex][move.from][move.to]

    history[colorIndex][move.from][move.to] = (current + bonus).coerceAtMost(HISTORY_MAX)
  }

  private fun moveScore(move: IntMove, ttMove: IntMove?, ply: Int): Int {
    if (move == ttMove) return TT_MOVE_SCORE

    val attacker = game.getPiece(move.from) ?: return 0
    val victim = game.getPiece(move.to)

    if (victim != null) {
      return CAPTURE_SCORE + pieceValue(victim) * 10 - pieceValue(attacker)
    }

    val killerScore = killerScore(move, ply)
    val historyScore = history[attacker.color.ordinal][move.from][move.to]
    return killerScore + historyScore + positionalMoveScore(move, attacker)
  }

  private fun killerScore(move: IntMove, ply: Int): Int {
    if (ply !in 0 until MAX_PLY) return 0
    return when (move) {
      killers[ply][0] -> KILLER_1_SCORE
      killers[ply][1] -> KILLER_2_SCORE
      else -> 0
    }
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
        val earlyQueenMove = (fromRank == 0 && toRank <= 2) || (fromRank == 7 && toRank >= 5)
        if (earlyQueenMove) -20 else 0
      }
      is King -> {
        if (abs(move.to - move.from) == 2) 80 else 0
      }
      else -> 0
    }

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

  private fun clearKillers() {
    for (ply in 0 until MAX_PLY) {
      killers[ply][0] = null
      killers[ply][1] = null
    }
  }

  private fun clearHistory() {
    for (color in history.indices) {
      for (from in 0..63) {
        history[color][from].fill(0)
      }
    }
  }

  companion object {
    const val MAX_PLY = 64

    private const val TT_MOVE_SCORE = 10_000_000
    private const val CAPTURE_SCORE = 1_000_000
    private const val KILLER_1_SCORE = 900_000
    private const val KILLER_2_SCORE = 800_000
    private const val HISTORY_MAX = 700_000

    private val START_MINOR_SQUARES = setOf(1, 2, 5, 6, 57, 58, 61, 62)
  }
}