package krilovs.andrejs.chess.dto

import krilovs.andrejs.chess.domain.model.Move

sealed interface PromotionResult {
  data class Success(val move: Move) : PromotionResult
  data class Error(val message: String) : PromotionResult
}