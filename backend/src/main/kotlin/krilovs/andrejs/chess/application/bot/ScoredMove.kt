package krilovs.andrejs.chess.application.bot

import krilovs.andrejs.chess.domain.model.Move

data class ScoredMove(
  val move: Move?,
  val score: Int
)