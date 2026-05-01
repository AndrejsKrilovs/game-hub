package krilovs.andrejs.chess.dto

import krilovs.andrejs.chess.game.Move

sealed interface MoveResult {
  data class Success(val move: Move) : MoveResult
  data class Error(val message: String) : MoveResult
  data class Promotion(val availablePieces: Set<String>, val move: Move) : MoveResult
}