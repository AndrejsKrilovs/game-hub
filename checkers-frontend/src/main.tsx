import { bootstrapGame } from 'shared-frontend';

import './checkers.css';
import { formatCheckersMove } from './game/CheckersHistoryWriter';
import { CheckersBoard } from './board/CheckersBoard';

bootstrapGame('checkers', {
  historyFormatter: formatCheckersMove,
  renderGame: ({ eventBus }) => <CheckersBoard eventBus={eventBus} />,
});
