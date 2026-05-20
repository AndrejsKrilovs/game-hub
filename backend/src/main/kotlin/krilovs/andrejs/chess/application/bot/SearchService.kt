package krilovs.andrejs.chess.application.bot

import krilovs.andrejs.chess.application.GameService
import krilovs.andrejs.chess.domain.model.Color
import krilovs.andrejs.chess.domain.model.GameState
import org.springframework.stereotype.Component

@Component
class SearchService(
  private val game: GameService,
  private val evaluation: EvaluationService,
  private val transpositionTable: TranspositionTable,
  private val positionHash: PositionHashService,
  private val moveOrdering: MoveOrderingService
) {
  private var deadlineMs: Long = 0
  fun reset() {
    transpositionTable.clear()
    moveOrdering.clear()
  }


  fun searchBestMove(maxDepth: Int, timeLimitMs: Long, botColor: Color): SearchResult {
    deadlineMs = System.currentTimeMillis() + timeLimitMs
    var bestResult = SearchResult(move = null, score = 0)

    for (depth in 1..maxDepth) {
      if (timeExpired()) break

      val result = searchMoves(
        depth = depth,
        ply = 0,
        alpha = -INF,
        beta = INF,
        botColor = botColor,
        ttMove = null
      )

      if (!timeExpired() && result.move != null) {
        bestResult = result
      }
    }

    return bestResult
  }

  private fun timeExpired(): Boolean =
    System.currentTimeMillis() >= deadlineMs

  private fun alphaBeta(depth: Int, ply: Int, alpha: Int, beta: Int, botColor: Color): Int {
    if (depth == 0) {
      return quiescence(alpha, beta, botColor)
    }

    val key = positionHash.key()
    var a = alpha
    var b = beta
    val originalAlpha = alpha

    val cached = transpositionTable.get(key)
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
      ply = ply,
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

    transpositionTable.put(
      key,
      TTEntry(
        depth = depth,
        score = score,
        flag = flag,
        bestMove = result.move
      )
    )

    return score
  }

  private fun searchMoves(depth: Int, ply: Int, alpha: Int, beta: Int, botColor: Color, ttMove: IntMove?): SearchResult {
    val moves = moveOrdering.orderedMoves(ttMove, ply)
    if (moves.isEmpty()) {
      return SearchResult(move = null, score = terminalOrStaticScore(botColor, ply))
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
        ply = ply + 1,
        alpha = a,
        beta = b,
        botColor = botColor
      )

      game.undoMove(undo)

      val better = if (maximizing) score > bestScore else score < bestScore
      if (better) {
        bestScore = score
        bestMove = move
      }
      if (maximizing) {
        a = maxOf(a, bestScore)
      }
      else {
        b = minOf(b, bestScore)
      }

      if (a >= b) {
        if (!moveOrdering.isCapture(move)) {
          moveOrdering.storeKiller(move, ply)
          moveOrdering.addHistoryBonus(move, depth)
        }

        break
      }
    }

    return SearchResult(move = bestMove, score = bestScore)
  }

  private fun terminalOrStaticScore(botColor: Color, ply: Int): Int {
    return when (game.getGameState()) {
      GameState.CHECKMATE -> if (game.currentTurn == botColor) -MATE_SCORE + ply else MATE_SCORE - ply
      GameState.STALEMATE, GameState.DRAW -> 0
      else -> evaluatePosition(botColor)
    }
  }

  private fun quiescence(alpha: Int, beta: Int, botColor: Color): Int {
    var a = alpha
    var b = beta
    val standPat = evaluatePosition(botColor)
    val maximizing = game.currentTurn == botColor

    if (maximizing) {
      if (standPat >= b) return b
      if (standPat > a) a = standPat
    }
    else {
      if (standPat <= a) return a
      if (standPat < b) b = standPat
    }

    val captures = moveOrdering
      .orderedMoves(ttMove = null, ply = 0)
      .filter { move -> moveOrdering.isCapture(move) }

    for (move in captures) {
      val undo = game.makeMoveInternal(move.from, move.to)
      val score = quiescence(a, b, botColor)
      game.undoMove(undo)

      if (maximizing) {
        if (score > a) a = score
        if (a >= b) return b
      }
      else {
        if (score < b) b = score
        if (a >= b) return a
      }
    }

    return if (maximizing) a else b
  }

  private fun evaluatePosition(botColor: Color): Int =
    evaluation.evaluate(game.getBoard(), botColor)

  companion object {
    private const val INF = Int.MAX_VALUE - 1
    private const val MATE_SCORE = 1_000_000
  }
}