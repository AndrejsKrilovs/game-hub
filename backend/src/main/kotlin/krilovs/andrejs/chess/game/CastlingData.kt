package krilovs.andrejs.chess.game

data class CastlingData(
  val kingStart: Int,
  val emptyShort: List<Int>,
  val emptyLong: List<Int>,
  val kingMoved: Boolean,
  val rookAMoved: Boolean,
  val rookHMoved: Boolean
)