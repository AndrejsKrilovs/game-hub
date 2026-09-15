import { useEffect, useRef } from 'react';
import { fromEventPattern, merge, tap } from 'rxjs';
import { EventBus } from 'shared-frontend';

interface CellClickPayload {
  cord: string;
}

interface StartGamePayload {
  color: string;
}

interface WsStatePayload {
  state?: 'CHECK' | 'DRAW' | 'STALEMATE' | 'CHECKMATE' | string;
  turn: 'WHITE' | 'BLACK';
  pieces: any[];
}

export function useGameController(eventBus: EventBus) {
  const selectedCellRef = useRef<string | null>(null);
  const lastMoveRef = useRef<any | null>(null);

  useEffect(() => {
    const listen = <T = any>(event: string) =>
      fromEventPattern<T>(
        (handler) => eventBus.on(event, handler),
        (_, signal) => signal()
      );

    const endGame$ = listen('END_GAME').pipe(
      tap(() => eventBus.emit('WS_SEND', { type: 'END_GAME' }))
    );

    const startGame$ = listen<StartGamePayload>('START_GAME').pipe(
      tap(({ color }) => {
        selectedCellRef.current = null;
        lastMoveRef.current = null;
        eventBus.emit('WS_SEND', { type: 'START_GAME', payload: { color } });
      })
    );

    const cellClick$ = listen<CellClickPayload>('CELL_CLICK').pipe(
      tap(({ cord }) => {
        const currentSelected = selectedCellRef.current;

        if (!currentSelected) {
          selectedCellRef.current = cord;
          eventBus.emit('WS_SEND', { type: 'GET_MOVES', payload: { from: cord } });
          return;
        }

        if (cord === currentSelected) {
          selectedCellRef.current = null;
          eventBus.emit('CLEAR_HIGHLIGHTS');
          return;
        }

        eventBus.emit('CLEAR_HIGHLIGHTS');
        eventBus.emit('WS_SEND', {
          type: 'MAKE_MOVE',
          payload: { from: currentSelected, to: cord },
        });
        selectedCellRef.current = null;
      })
    );

    const wsMove$ = listen<{ move: any }>('WS:MOVE').pipe(
      tap(({ move }) => {
        lastMoveRef.current = move;
      })
    );

    const wsGameEnded$ = listen<{ message?: string } | string>('WS:GAME_ENDED').pipe(
      tap((payload) => {
        const message = typeof payload === 'object' ? payload?.message : payload;
        eventBus.emit('GAME_ENDED', { message: message ?? 'Партия завершена' });
      })
    );

    const wsMoves$ = listen<{ moves: any[] }>('WS:MOVES').pipe(
      tap(({ moves }) => eventBus.emit('HIGHLIGHT_MOVES', moves))
    );

    const wsError$ = listen<{ message: string }>('WS:ERROR').pipe(
      tap(({ message }) => {
        selectedCellRef.current = null;
        eventBus.emit('TOAST', { message });
      })
    );

    const wsState$ = listen<WsStatePayload>('WS:STATE').pipe(
      tap(({ state, turn, pieces }) => {
        eventBus.emit('UPDATE_BOARD', { turn, pieces });

        if (lastMoveRef.current) {
          eventBus.emit('ADD_HISTORY', { ...lastMoveRef.current, state });
          lastMoveRef.current = null;
        }

        const winnerColor = turn === 'WHITE' ? 'чёрных' : 'белых';

        switch (state) {
          case 'CHECK':
            eventBus.emit('TOAST', { message: 'ШАХ!' });
            break;
          case 'DRAW':
          case 'STALEMATE': {
            const drawMsg = 'Партия завершилась в ничью!';
            eventBus.emit('GAME_ENDED', { message: drawMsg });
            eventBus.emit('TOAST', { message: drawMsg });
            eventBus.emit('ADD_HISTORY', { text: drawMsg });
            break;
          }
          case 'CHECKMATE': {
            const winMsg = `Партия завершилась победой ${winnerColor}!`;
            eventBus.emit('GAME_ENDED', { message: winMsg });
            eventBus.emit('CONFETTI');
            eventBus.emit('TOAST', { message: winMsg });
            eventBus.emit('ADD_HISTORY', { text: winMsg });
            break;
          }
        }
      })
    );

    const subscription = merge(
      endGame$,
      startGame$,
      cellClick$,
      wsMove$,
      wsGameEnded$,
      wsMoves$,
      wsError$,
      wsState$
    ).subscribe();

    return () => subscription.unsubscribe();
  }, [eventBus]);
}