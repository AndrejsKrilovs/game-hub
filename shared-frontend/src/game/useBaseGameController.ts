import { useEffect } from 'react';
import { merge, tap } from 'rxjs';

import type { EventBus } from './EventBus';
import { APP_EVENTS } from './events';
import { sendWsCommand } from './sendWsCommand';

interface ServerMessagePayload {
  message?: string;
}

interface InviteCreatedPayload {
  inviteUrl: string;
}

const getMessage = (payload: ServerMessagePayload | string | undefined, fallback: string): string => {
  if (typeof payload === 'string') {
    return payload;
  }

  return payload?.message ?? fallback;
};

/** Общая для всех игр логика: команды старта/приглашения/завершения и серверные события жизненного цикла. */
export const useBaseGameController = (eventBus: EventBus): void => {
  useEffect(() => {
    const subscription = merge(
      eventBus.observe(APP_EVENTS.startGame).pipe(tap((payload) => sendWsCommand(eventBus, 'START_GAME', payload))),
      eventBus.observe(APP_EVENTS.invite).pipe(tap((payload) => sendWsCommand(eventBus, 'INVITE', payload))),
      eventBus.observe(APP_EVENTS.endGame).pipe(tap(() => sendWsCommand(eventBus, 'END_GAME'))),

      eventBus.observe<ServerMessagePayload | string>(APP_EVENTS.wsGameEnded).pipe(
        tap((payload) => {
          eventBus.emit(APP_EVENTS.gameEnded, { message: getMessage(payload, 'Партия завершена') });
          // Сервер закрывает сокет после GAME_ENDED, переподключаться не нужно
          eventBus.emit(APP_EVENTS.wsDisconnect);
        })
      ),

      eventBus.observe<ServerMessagePayload>(APP_EVENTS.wsServerError).pipe(
        tap((payload) => eventBus.emit(APP_EVENTS.toast, { message: payload?.message ?? 'Ошибка игры', type: 'error' }))
      ),

      eventBus.observe<InviteCreatedPayload>(APP_EVENTS.wsInviteCreated).pipe(
        tap(({ inviteUrl }) => {
          void navigator.clipboard?.writeText(inviteUrl).catch(() => undefined);
          eventBus.emit(APP_EVENTS.toast, { message: 'Ссылка приглашения скопирована в буфер обмена', type: 'success' });
        })
      ),

      eventBus.observe<ServerMessagePayload>(APP_EVENTS.wsWaitingForOpponent).pipe(
        tap((payload) => {
          const message = `${payload?.message}\n`;
          eventBus.emit(APP_EVENTS.toast, { message, type: 'info' });
          eventBus.emit(APP_EVENTS.addHistory, { text: message });
        })
      )
    ).subscribe();

    return () => subscription.unsubscribe();
  }, [eventBus]);
};
