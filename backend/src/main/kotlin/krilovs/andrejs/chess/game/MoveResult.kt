package krilovs.andrejs.chess.game

sealed interface MoveResult {
  data class Success(val moves: Set<Move>) : MoveResult
  data class Error(val message: String) : MoveResult
}