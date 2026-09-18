import { EMPTY, merge, Observable, Subject } from 'rxjs';

export type EventHandler<T = unknown> = (payload?: T) => void;
export type Unsubscribe = () => void;
export type EventPattern = string | readonly string[];

const EVENT_SEPARATOR = '|';

export const toEventList = (event: EventPattern): string[] => {
  return typeof event === 'string' ? [event] : [...event];
};

export const toEventKey = (event: EventPattern): string => {
  return toEventList(event).join(EVENT_SEPARATOR);
};

export class EventBus {
  private static instance?: EventBus;

  private readonly subjects = new Map<string, Subject<unknown>>();

  private constructor() {}

  static getInstance = (): EventBus => {
    if (!EventBus.instance) {
      EventBus.instance = new EventBus();
    }

    return EventBus.instance;
  };

  emit = <T = unknown>(event: string, payload?: T): void => {
    this.getSubject(event).next(payload);
  };

  on = <T = unknown>(event: string, handler: EventHandler<T>): Unsubscribe => {
    const subscription = this.observe<T>(event).subscribe((payload) => {
      handler(payload);
    });

    return () => {
      subscription.unsubscribe();
    };
  };

  observe = <T = unknown>(event: string): Observable<T> => {
    return this.getSubject(event).asObservable() as Observable<T>;
  };

  observePattern = <T = unknown>(event: EventPattern): Observable<T> => {
    const eventList = toEventList(event);

    if (eventList.length === 0) {
      return EMPTY as Observable<T>;
    }

    return merge(...eventList.map((eventName) => this.observe<T>(eventName)));
  };

  clear = (event?: string): void => {
    if (event) {
      this.subjects.get(event)?.complete();
      this.subjects.delete(event);
      return;
    }

    this.subjects.forEach((subject) => subject.complete());
    this.subjects.clear();
  };

  private getSubject = (event: string): Subject<unknown> => {
    let subject = this.subjects.get(event);

    if (!subject) {
      subject = new Subject<unknown>();
      this.subjects.set(event, subject);
    }

    return subject;
  };
}

export const eventBus = EventBus.getInstance();