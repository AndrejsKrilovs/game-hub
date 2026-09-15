import { createRoot } from 'react-dom/client';
import { bootstrapGame } from 'shared-frontend';
import 'shared-frontend/style.css';
import 'shared-frontend/mobile.css';

import { formatChessMove } from './game/ChessHistoryWriter';
import { ChessBoard } from './board/ChessBoard';
import { PromotionModal } from './game/Promotion.tsx';

const { eventBus, appContainer } = bootstrapGame('chess', formatChessMove);

const root = createRoot(appContainer);
root.render(
  <>
    <ChessBoard eventBus={eventBus} />
    <PromotionModal eventBus={eventBus} />
  </>
);