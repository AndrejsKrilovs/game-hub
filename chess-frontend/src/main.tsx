import { bootstrapGame } from 'shared-frontend';

import { formatChessMove } from './game/ChessHistoryWriter';
import { ChessBoard } from './board/ChessBoard';
import { PromotionModal } from './game/Promotion.tsx';

bootstrapGame('chess', {
  historyFormatter: formatChessMove,

  renderGame: ({ eventBus }) => (
    <>
      <ChessBoard eventBus={eventBus} />
      <PromotionModal eventBus={eventBus} />
    </>
  ),
});