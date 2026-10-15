import { useEffect, useRef } from 'react';
import { merge, tap } from 'rxjs';
import { APP_EVENTS, sendWsCommand, type EventBus } from 'shared-frontend';

import { CHECKERS_EVENTS } from '../events';
import type { PieceColor, PieceType } from '../board/BoardTypes';

interface CellClickPayload {
  coord?: string;
}

interface WsStatePayload {
  state?: 'NORMAL' | 'WIN' | string;
  turn: PieceColor;
  winner?: PieceColor;
  pieces: PieceType[];
}

interface WsMove {
  from: string;
  to: string;
  color: PieceColor;
  continues?: boolean;
}

interface WsMovePayload {
  move: WsMove;
}

interface WsMovesPayload {
  moves: { to: string }[];
}

interface StartGamePayload {
  color?: PieceColor;
}

interface WsPlayerColorPayload {
  color: PieceColor;
}

const MOVE_COMMANDS = {
  getMoves: 'GET_MOVES',
  makeMove: 'MAKE_MOVE',
} as const;

export const useCheckersController = (eventBus: EventBus): void => {
  const selectedCellRef = useRef<string | null>(null);
  const lastMoveRef = useRef<WsMove | null>(null);
  const playerColorRef = useRef<PieceColor | null>(null);
  const turnRef = useRef<PieceColor>('WHITE');
  const piecesRef = useRef<Map<string, PieceType>>(new Map());
  const chainRef = useRef(false);

  useEffect(() => {
    const sendWs = (type: string, payload?: unknown): void => sendWsCommand(eventBus, type, payload);

    const resetState = (): void => {
      selectedCellRef.current = null;
      lastMoveRef.current = null;
      playerColorRef.current = null;
      turnRef.current = 'WHITE';
      piecesRef.current = new Map();
      chainRef.current = false;
    };

    const clearSelection = (): void => {
      selectedCellRef.current = null;
      eventBus.emit(CHECKERS_EVENTS.clearHighlights);
    };

    const select = (coord: string): void => {
      selectedCellRef.current = coord;
      sendWs(MOVE_COMMANDS.getMoves, { from: coord });
    };

    const addLastMoveToHistory = (state?: string): void => {
      if (!lastMoveRef.current) {
        return;
      }

      eventBus.emit(APP_EVENTS.addHistory, { ...lastMoveRef.current, state });
      lastMoveRef.current = null;
    };

    const finishGame = (message: string, type: 'info' | 'success' = 'info', withConfetti = false): void => {
      eventBus.emit(APP_EVENTS.gameEnded, { message });
      eventBus.emit(APP_EVENTS.toast, { message, type });

      if (withConfetti) {
        eventBus.emit(APP_EVENTS.confetti);
      }
    };

    const isOwnTurnPiece = (coord: string): boolean => {
      const piece = piecesRef.current.get(coord);
      return !!piece && piece.color === turnRef.current && !piece.captured;
    };

    const handleCellClick = ({ coord }: CellClickPayload): void => {
      if (!coord) {
        return;
      }

      const selectedCell = selectedCellRef.current;

      if (!selectedCell) {
        select(coord);
        return;
      }

      if (coord === selectedCell) {
        // Во время серии взятий снять выбор нельзя: ход обязан продолжить эта же шашка
        if (!chainRef.current) {
          clearSelection();
        }
        return;
      }

      // Выбор другой своей шашки вместо хода (кроме серии взятий)
      if (!chainRef.current && isOwnTurnPiece(coord)) {
        eventBus.emit(CHECKERS_EVENTS.clearHighlights);
        select(coord);
        return;
      }

      eventBus.emit(CHECKERS_EVENTS.clearHighlights);
      sendWs(MOVE_COMMANDS.makeMove, { from: selectedCell, to: coord });
      selectedCellRef.current = null;
    };

    const handleMove = ({ move }: WsMovePayload): void => {
      lastMoveRef.current = move;
      chainRef.current = !!move.continues;

      const isMine = playerColorRef.current === move.color;
      if (move.continues && isMine) {
        // Серия взятий: сразу выбираем шашку, которая должна бить дальше
        select(move.to);
      }
    };

    const handleState = ({ state, turn, winner, pieces }: WsStatePayload): void => {
      turnRef.current = turn;
      piecesRef.current = new Map(pieces.map((piece) => [piece.coordinates, piece]));

      eventBus.emit(CHECKERS_EVENTS.updateBoard, { turn, pieces });
      addLastMoveToHistory(state);

      if (state === 'WIN') {
        const winnerColor = winner === 'WHITE' ? 'белых' : 'чёрных';
        finishGame(`Партия завершилась победой ${winnerColor}!`, 'success', true);
      }
    };

    const subscription = merge(
      eventBus.observe<StartGamePayload | undefined>(APP_EVENTS.startGame).pipe(
        tap((payload) => {
          resetState();
          // Игра с ботом: цвет игрока известен сразу, PLAYER_COLOR сервер не присылает
          playerColorRef.current = payload?.color ?? null;
        })
      ),

      eventBus.observe(APP_EVENTS.invite).pipe(
        tap(resetState)
      ),

      eventBus.observe<WsPlayerColorPayload>(CHECKERS_EVENTS.wsPlayerColor).pipe(
        tap(({ color }) => {
          playerColorRef.current = color;
        })
      ),

      eventBus.observe(APP_EVENTS.wsServerError).pipe(
        tap(() => {
          // В серии взятий выбор сохраняется, иначе пользователь застрянет
          if (!chainRef.current) {
            selectedCellRef.current = null;
          }
        })
      ),

      eventBus.observe<CellClickPayload>(CHECKERS_EVENTS.cellClick).pipe(
        tap(handleCellClick)
      ),

      eventBus.observe<WsMovePayload>(CHECKERS_EVENTS.wsMove).pipe(
        tap(handleMove)
      ),

      eventBus.observe<WsMovesPayload>(CHECKERS_EVENTS.wsMoves).pipe(
        tap(({ moves }) => {
          eventBus.emit(CHECKERS_EVENTS.highlightMoves, moves);
        })
      ),

      eventBus.observe<WsStatePayload>(APP_EVENTS.wsState).pipe(
        tap(handleState)
      )
    ).subscribe();

    return () => subscription.unsubscribe();
  }, [eventBus]);
};
