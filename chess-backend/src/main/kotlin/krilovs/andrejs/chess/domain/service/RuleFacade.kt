package krilovs.andrejs.chess.domain.service

class RuleFacade(
  val moveSafety: MoveSafetyService,
  val castling: CastlingService,
  val gameState: GameStateService,
  val promotion: PromotionService
)