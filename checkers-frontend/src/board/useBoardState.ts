import { APP_EVENTS, type EventBus, useEventReducer, type EventReducerBinding } from 'shared-frontend';
import { CHECKERS_EVENTS } from '../events';
import type { PieceType, BoardPerspective, MoveHighlight } from './BoardTypes';

interface BoardState {
  perspective: BoardPerspective;
  pieces: PieceType[];
  highlights: Set<string>;
  isFinished: boolean;
  isStarted: boolean;
}

type BoardAction =
  | { type: 'START_GAME'; payload?: { color?: BoardPerspective } }
  | { type: 'PLAYER_COLOR'; payload: { color: BoardPerspective } }
  | { type: 'UPDATE_BOARD'; payload?: { pieces?: PieceType[] } }
  | { type: 'HIGHLIGHT_MOVES'; payload: MoveHighlight[] }
  | { type: 'CLEAR_HIGHLIGHTS' }
  | { type: 'GAME_ENDED' }
  | { type: 'RESET' };

type BoardActionType = BoardAction['type'];
type ActionOf<T extends BoardActionType> = Extract<BoardAction, { type: T }>;

const createInitialState = (): BoardState => ({
  perspective: 'WHITE',
  pieces: [],
  highlights: new Set(),
  isFinished: false,
  isStarted: false,
});

const bindPayload = <T extends BoardActionType>(event: string, type: T): EventReducerBinding<BoardAction> => ({
  event,
  toAction: (payload) => ({ type, payload } as ActionOf<T>),
});

const bind = <T extends BoardActionType>(event: string, type: T): EventReducerBinding<BoardAction> => ({
  event,
  toAction: () => ({ type } as ActionOf<T>),
});

const boardBindings: EventReducerBinding<BoardAction>[] = [
  bindPayload(APP_EVENTS.startGame, 'START_GAME'),
  bindPayload(CHECKERS_EVENTS.wsPlayerColor, 'PLAYER_COLOR'),
  bindPayload(CHECKERS_EVENTS.updateBoard, 'UPDATE_BOARD'),
  bindPayload(CHECKERS_EVENTS.highlightMoves, 'HIGHLIGHT_MOVES'),

  bind(CHECKERS_EVENTS.clearHighlights, 'CLEAR_HIGHLIGHTS'),
  bind(APP_EVENTS.gameEnded, 'GAME_ENDED'),
  bind(APP_EVENTS.wsGameEnded, 'GAME_ENDED'),
  bind(APP_EVENTS.gameExit, 'RESET'),
];

const boardReducer = (state: BoardState, action: BoardAction): BoardState => {
  switch (action.type) {
    case 'START_GAME':
      return {
        ...createInitialState(),
        perspective: action.payload?.color ?? 'WHITE',
        isStarted: true,
      };
    case 'PLAYER_COLOR':
      return {
        ...state,
        perspective: action.payload.color,
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
        highlights: new Set(action.payload.map((move) => move.to)),
      };
    case 'CLEAR_HIGHLIGHTS':
      if (state.highlights.size === 0) {
        return state;
      }

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
      return createInitialState();
    default:
      return state;
  }
};

export const useBoardState = (eventBus: EventBus): BoardState => {
  const [state] = useEventReducer(eventBus, boardReducer, createInitialState, boardBindings);
  return state;
};
