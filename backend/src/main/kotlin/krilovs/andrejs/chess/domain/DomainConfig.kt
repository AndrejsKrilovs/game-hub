package krilovs.andrejs.chess.domain

import krilovs.andrejs.chess.domain.service.CastlingService
import krilovs.andrejs.chess.domain.service.GameStateService
import krilovs.andrejs.chess.domain.service.MoveSafetyService
import krilovs.andrejs.chess.domain.service.PromotionService
import krilovs.andrejs.chess.domain.service.RuleFacade
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class DomainConfig {
  @Bean
  open fun moveSafetyService() = MoveSafetyService()

  @Bean
  open fun promotionService() = PromotionService()

  @Bean
  open fun castlingService(moveSafety: MoveSafetyService) = CastlingService(moveSafety)

  @Bean
  open fun gameStateService(moveSafety: MoveSafetyService) = GameStateService(moveSafety)

  @Bean
  open fun ruleFacade(
    moveSafety: MoveSafetyService,
    castling: CastlingService,
    gameState: GameStateService,
    promotion: PromotionService
  ) = RuleFacade(moveSafety, castling, gameState, promotion)
}