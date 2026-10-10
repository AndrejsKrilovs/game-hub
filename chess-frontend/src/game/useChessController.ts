import { useEffect, useRef } from 'react';
import { merge, tap } from 'rxjs';
import { APP_EVENTS, sendWsCommand, type EventBus } from 'shared-frontend';

import { CHESS_EVENTS } from '../events';

interface CellClickPayload {
  coord?: string;
}

interface WsStatePayload {
  state?: 'CHECK' | 'DRAW' | 'STALEMATE' | 'CHECKMATE' | string;
  turn: 'WHITE' | 'BLACK';
  pieces: any[];
}

interface WsMovePayload {
  move: any;
}

interface WsMovesPayload {
  moves: any[];
}

const MOVE_COMMANDS = {
  getMoves: 'GET_MOVES',
  makeMove: 'MAKE_MOVE',
} as const;

export const useChessController = (eventBus: EventBus): void => {
  const selectedCellRef = useRef<string | null>(null);
  const lastMoveRef = useRef<any | null>(null);

  useEffect(() => {
    const sendWs = (type: string, payload?: unknown): void => sendWsCommand(eventBus, type, payload);

    const resetChessState = (): void => {
      selectedCellRef.current = null;
      lastMoveRef.current = null;
    };

    const clearSelection = (): void => {
      selectedCellRef.current = null;
      eventBus.emit(CHESS_EVENTS.clearHighlights);
    };

    const addLastMoveToHistory = (state?: string): void => {
      if (!lastMoveRef.current) {
        return;
      }

      eventBus.emit(APP_EVENTS.addHistory, {
        ...lastMoveRef.current,
        state,
      });

      lastMoveRef.current = null;
    };

    const finishGame = (
      message: string,
      type: 'info' | 'success' = 'info',
      withConfetti = false
    ): void => {
      eventBus.emit(APP_EVENTS.gameEnded, { message });
      eventBus.emit(APP_EVENTS.toast, { message, type });

      if (withConfetti) {
        eventBus.emit(APP_EVENTS.confetti);
      }
    };

    const handleCellClick = ({ coord }: CellClickPayload): void => {
      if (!coord) {
        return;
      }

      const selectedCell = selectedCellRef.current;

      if (!selectedCell) {
        selectedCellRef.current = coord;
        sendWs(MOVE_COMMANDS.getMoves, { from: coord });
        return;
      }

      if (coord === selectedCell) {
        clearSelection();
        return;
      }

      eventBus.emit(CHESS_EVENTS.clearHighlights);

      sendWs(MOVE_COMMANDS.makeMove, {
        from: selectedCell,
        to: coord,
      });

      selectedCellRef.current = null;
    };

    const handleState = ({ state, turn, pieces }: WsStatePayload): void => {
      eventBus.emit(CHESS_EVENTS.updateBoard, { turn, pieces });

      addLastMoveToHistory(state);

      if (state === 'CHECK') {
        eventBus.emit(APP_EVENTS.toast, {
          message: 'ШАХ!',
          type: 'info',
        });

        return;
      }

      if (state === 'DRAW' || state === 'STALEMATE') {
        finishGame('Партия завершилась в ничью!');
        return;
      }

      if (state === 'CHECKMATE') {
        const winnerColor = turn === 'WHITE' ? 'чёрных' : 'белых';

        finishGame(
          `Партия завершилась победой ${winnerColor}!`,
          'success',
          true
        );
      }
    };

    const subscription = merge(
      merge(eventBus.observe(APP_EVENTS.startGame), eventBus.observe(APP_EVENTS.invite)).pipe(
        tap(resetChessState)
      ),

      eventBus.observe(APP_EVENTS.wsServerError).pipe(
        tap(() => {
          selectedCellRef.current = null;
        })
      ),

      eventBus.observe<CellClickPayload>(CHESS_EVENTS.cellClick).pipe(
        tap(handleCellClick)
      ),

      eventBus.observe<WsMovePayload>(CHESS_EVENTS.wsMove).pipe(
        tap(({ move }) => {
          lastMoveRef.current = move;
        })
      ),

      eventBus.observe<WsMovesPayload>(CHESS_EVENTS.wsMoves).pipe(
        tap(({ moves }) => {
          eventBus.emit(CHESS_EVENTS.highlightMoves, moves);
        })
      ),

      eventBus.observe<WsStatePayload>(APP_EVENTS.wsState).pipe(
        tap(handleState)
      )
    ).subscribe();

    return () => {
      subscription.unsubscribe();
    };
  }, [eventBus]);
};