import { useEffect, useRef } from 'react';
import { EventBus } from './EventBus';

type EventPattern = string | string[];

export function useEvent<T = any>(
  eventBus: EventBus,
  event: EventPattern,
  handler: (payload: T) => void
) {
  const savedHandler = useRef(handler);

  useEffect(() => {
    savedHandler.current = handler;
  }, [handler]);

  useEffect(() => {
    const eventList = Array.isArray(event) ? event : [event];
    const unsubscribers = eventList.map((e) =>
      eventBus.on(e, (payload) => savedHandler.current(payload as T))
    );

    return () => unsubscribers.forEach((unsub) => unsub());
  }, [eventBus, Array.isArray(event) ? event.join(',') : event]);
}