import { useEffect, useReducer, useRef, type Dispatch } from 'react';
import { merge, map } from 'rxjs';

import type { EventBus } from './EventBus';

export interface EventReducerBinding<TAction> {
  event: string;
  toAction: (payload: unknown) => TAction;
}

export const useEventReducer = <TState, TAction>(
  eventBus: EventBus,
  reducer: (state: TState, action: TAction) => TState,
  createInitialState: () => TState,
  bindings: EventReducerBinding<TAction>[]
): [TState, Dispatch<TAction>] => {
  const [state, dispatch] = useReducer(reducer, undefined, createInitialState);
  const bindingsRef = useRef(bindings);

  useEffect(() => {
    const subscription = merge(
      ...bindingsRef.current.map(({ event, toAction }) => eventBus.observe(event).pipe(map(toAction)))
    ).subscribe(dispatch);

    return () => subscription.unsubscribe();
  }, [eventBus]);

  return [state, dispatch];
};
