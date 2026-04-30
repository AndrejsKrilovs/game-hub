package krilovs.andrejs.chess.dto

import krilovs.andrejs.chess.game.Move

sealed interface AvailableMovesResult {
  data class Success(val moves: Set<Move>) : AvailableMovesResult
  data class Error(val message: String) : AvailableMovesResult
}