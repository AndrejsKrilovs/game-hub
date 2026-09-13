import { useReducer, useEffect } from 'react';
import { fromEventPattern, merge, map } from 'rxjs';
import { EventBus } from 'shared-frontend';
import { PieceType, BoardPerspective, MoveHighlight } from './BoardTypes';

interface BoardState {
  perspective: BoardPerspective;
  pieces: PieceType[];
  highlights: Set<string>;
  isFinished: boolean;
  isStarted: boolean;
}

type BoardAction =
  | { type: 'START_GAME'; payload?: { color?: BoardPerspective } }
  | { type: 'UPDATE_BOARD'; payload?: { pieces?: PieceType[] } }
  | { type: 'HIGHLIGHT_MOVES'; payload: MoveHighlight[] }
  | { type: 'CLEAR_HIGHLIGHTS' }
  | { type: 'GAME_ENDED' }
  | { type: 'RESET' };

const initialState: BoardState = {
  perspective: 'WHITE',
  pieces: [],
  highlights: new Set(),
  isFinished: false,
  isStarted: false
};

const boardReducer = (state: BoardState, action: BoardAction): BoardState => {
  switch (action.type) {
    case 'START_GAME':
      return {
        ...state,
        perspective: action.payload?.color ?? 'WHITE',
        isFinished: false,
        isStarted: true,
      };
    case 'UPDATE_BOARD':
      return {
        ...state,
        pieces: action.payload?.pieces ?? [],
        isStarted: true,
      };
    case 'HIGHLIGHT_MOVES':
      return {
        ...state,
        highlights: new Set((action.payload ?? []).map((m) => m.to)),
      };
    case 'CLEAR_HIGHLIGHTS':
      return {
        ...state,
        highlights: new Set(),
      };
    case 'GAME_ENDED':
      return {
        ...state,
        isFinished: true,
        highlights: new Set(),
      };
    case 'RESET':
      return initialState;
    default:
      return state;
  }
}

export const useBoardState = (eventBus: EventBus) => {
  const [state, dispatch] = useReducer(boardReducer, initialState);

  useEffect(() => {
    const listen = <T = any>(event: string) =>
      fromEventPattern<T>(
        (handler) => eventBus.on(event, handler),
        (_, signal) => signal()
      );

    const subscription = merge(
      listen('START_GAME').pipe(map((payload) => ({ type: 'START_GAME' as const, payload }))),
      listen('UPDATE_BOARD').pipe(map((payload) => ({ type: 'UPDATE_BOARD' as const, payload }))),
      listen('HIGHLIGHT_MOVES').pipe(map((payload) => ({ type: 'HIGHLIGHT_MOVES' as const, payload: payload as MoveHighlight[] }))),
      listen('CLEAR_HIGHLIGHTS').pipe(map(() => ({ type: 'CLEAR_HIGHLIGHTS' as const }))),
      listen('GAME_ENDED').pipe(map(() => ({ type: 'GAME_ENDED' as const }))),
      listen('WS:GAME_ENDED').pipe(map(() => ({ type: 'GAME_ENDED' as const }))),
      listen('GAME_EXIT').pipe(map(() => ({ type: 'RESET' as const })))
    ).subscribe((action) => dispatch(action as BoardAction));

    return () => subscription.unsubscribe();
  }, [eventBus]);

  return state;
}