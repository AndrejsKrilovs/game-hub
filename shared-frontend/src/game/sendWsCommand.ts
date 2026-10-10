import type { EventBus } from './EventBus';
import { APP_EVENTS } from './events';

export const sendWsCommand = (eventBus: EventBus, type: string, payload?: unknown): void => {
  eventBus.emit(APP_EVENTS.wsSend, { type, payload });
};
