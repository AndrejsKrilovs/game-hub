import { useEffect, useMemo, useRef } from 'react';
import type { EventBus, EventPattern } from './EventBus';
import { toEventKey } from './EventBus';

export const useEvent = <T = unknown>(eventBus: EventBus, event: EventPattern, handler: (payload: T) => void): void => {
  const savedHandler = useRef(handler);
  const eventKey = useMemo(() => toEventKey(event), [event]);

  useEffect(() => {
    savedHandler.current = handler;
  }, [handler]);

  useEffect(() => {
    const eventList = eventKey.split('|').filter(Boolean);
    const subscription = eventBus.observePattern<T>(eventList).subscribe((payload) => {
      savedHandler.current(payload);
    });

    return () => subscription.unsubscribe();
  }, [eventBus, eventKey]);
};