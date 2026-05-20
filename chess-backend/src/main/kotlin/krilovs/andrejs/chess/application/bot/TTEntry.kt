package krilovs.andrejs.chess.application.bot

data class TTEntry(
  val depth: Int,
  val score: Int,
  val flag: TTFlag,
  val bestMove: IntMove?
)
