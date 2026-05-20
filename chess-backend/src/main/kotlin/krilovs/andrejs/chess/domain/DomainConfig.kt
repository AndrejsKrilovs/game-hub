package krilovs.andrejs.chess.domain

import krilovs.andrejs.chess.domain.service.CastlingService
import krilovs.andrejs.chess.domain.service.GameStateService
import krilovs.andrejs.chess.domain.service.MoveSafetyService
import krilovs.andrejs.chess.domain.service.PromotionService
import krilovs.andrejs.chess.domain.service.RuleFacade
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DomainConfig {
  @Bean
  fun moveSafetyService() = MoveSafetyService()

  @Bean
  fun promotionService() = PromotionService()

  @Bean
  fun castlingService(moveSafety: MoveSafetyService) = CastlingService(moveSafety)

  @Bean
  fun gameStateService(moveSafety: MoveSafetyService) = GameStateService(moveSafety)

  @Bean
  fun ruleFacade(
    moveSafety: MoveSafetyService,
    castling: CastlingService,
    gameState: GameStateService,
    promotion: PromotionService
  ) = RuleFacade(moveSafety, castling, gameState, promotion)
}