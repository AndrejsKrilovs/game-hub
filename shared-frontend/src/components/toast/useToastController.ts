import { useCallback, useEffect, useState } from 'react';
import { merge, tap, timer } from 'rxjs';

import type { EventBus } from '../../game/EventBus';
import { APP_EVENTS } from '../../game/events';

export type ToastType = 'info' | 'success' | 'error';
export type ColorType = 'WHITE' | 'BLACK';

export type ToastState =
  | { kind: 'color' }
  | { kind: 'end' }
  | { kind: 'game_over'; text: string }
  | { kind: 'message'; text: string; type: ToastType }
  | null;

interface ToastPayload {
  message?: string;
  text?: string;
  type?: ToastType;
}

interface ToastControllerOptions {
  eventBus: EventBus;
}

export interface ToastController {
  toast: ToastState;
  selectedColor: ColorType | null;
  selectColor: (color: ColorType) => void;
  startGame: () => void;
  invite: () => void;
  confirmEnd: () => void;
  close: () => void;
}

const AUTO_HIDE_MESSAGE_MS = 3000;
const AUTO_HIDE_GAME_OVER_MS = 2000;

const getPayloadText = (payload?: ToastPayload): string => {
  return payload?.message ?? payload?.text ?? '';
};

export const useToastController = ({
                                     eventBus,
                                   }: ToastControllerOptions): ToastController => {
  const [toast, setToast] = useState<ToastState>(null);
  const [selectedColor, setSelectedColor] = useState<ColorType | null>(null);

  const close = useCallback(() => {
    setToast(null);
  }, []);

  const emitColorAction = useCallback(
    (eventName: typeof APP_EVENTS.startGame | typeof APP_EVENTS.invite) => {
      if (!selectedColor) {
        return;
      }

      eventBus.emit(eventName, { color: selectedColor });
      close();
    },
    [close, eventBus, selectedColor]
  );

  const startGame = useCallback(() => {
    emitColorAction(APP_EVENTS.startGame);
  }, [emitColorAction]);

  const invite = useCallback(() => {
    emitColorAction(APP_EVENTS.invite);
  }, [emitColorAction]);

  const confirmEnd = useCallback(() => {
    close();
    eventBus.emit(APP_EVENTS.endGame);
  }, [close, eventBus]);

  useEffect(() => {
    const subscription = merge(
      eventBus.observe(APP_EVENTS.showColorPicker).pipe(
        tap(() => {
          setSelectedColor(null);
          setToast({ kind: 'color' });
        })
      ),

      eventBus.observe(APP_EVENTS.showEndConfirm).pipe(
        tap(() => {
          setToast({ kind: 'end' });
        })
      ),

      eventBus.observe<ToastPayload>(APP_EVENTS.gameEnded).pipe(
        tap((payload) => {
          setToast({
            kind: 'game_over',
            text: getPayloadText(payload),
          });
        })
      ),

      eventBus.observe<ToastPayload>(APP_EVENTS.toast).pipe(
        tap((payload) => {
          setToast({
            kind: 'message',
            text: getPayloadText(payload),
            type: payload?.type ?? 'info',
          });
        })
      )
    ).subscribe();

    return () => {
      subscription.unsubscribe();
    };
  }, [eventBus]);

  useEffect(() => {
    if (toast?.kind === 'message') {
      const subscription = timer(AUTO_HIDE_MESSAGE_MS).subscribe(close);

      return () => {
        subscription.unsubscribe();
      };
    }

    if (toast?.kind === 'game_over') {
      const subscription = timer(AUTO_HIDE_GAME_OVER_MS).subscribe(() => {
        close();
        eventBus.emit(APP_EVENTS.wsDisconnect);
      });

      return () => {
        subscription.unsubscribe();
      };
    }
  }, [close, eventBus, toast]);

  return {
    toast,
    selectedColor,
    selectColor: setSelectedColor,
    startGame,
    invite,
    confirmEnd,
    close,
  };
};