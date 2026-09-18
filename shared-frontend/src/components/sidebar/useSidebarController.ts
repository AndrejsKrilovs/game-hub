import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type React from 'react';
import { merge, tap } from 'rxjs';

import type { EventBus } from '../../game/EventBus';
import { APP_EVENTS } from '../../game/events';
import { useAutoScroll } from '../../game/useAutoScroll';
import {useWindowDeviceType, type DeviceType } from '../../game/useWindowDeviceType';

export type HistoryFormatter = (payload: any) => string;
export type SidebarMode = 'idle' | 'joining' | 'playing' | 'finished';

interface SidebarControllerOptions {
  eventBus: EventBus;
  gameName: string;
  historyFormatter: HistoryFormatter;
}

interface HistoryPayload {
  text?: string;
  message?: string;
  resetControls?: boolean;
}

export interface SidebarController {
  mode: SidebarMode;
  historyText: string;
  textareaRef: React.RefObject<HTMLTextAreaElement | null>;
  startGame: () => void;
  endGame: () => void;
  goHome: () => void;
}

const HISTORY_LEFT_COLUMN_WIDTH = 30;

export const defaultHistoryFormatter: HistoryFormatter = (payload) => {
  if (payload?.text) {
    return String(payload.text);
  }

  return '';
};

const getGameSessionIdFromUrl = (): string | null => {
  const params = new URLSearchParams(window.location.search);
  return params.get('gameSessionId');
};

const removeGameSessionIdFromUrl = (): void => {
  const url = new URL(window.location.href);
  if (!url.searchParams.has('gameSessionId')) {
    return;
  }

  url.searchParams.delete('gameSessionId');
  window.history.replaceState(window.history.state, '', `${url.pathname}${url.search}${url.hash}`);
};

const buildWebSocketUrl = (gameName: string, gameSessionId: string | null): string => {
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const query = gameSessionId ? `?gameSessionId=${encodeURIComponent(gameSessionId)}` : '';
  return `${wsProtocol}//${window.location.host}/${gameName}/ws${query}`;
};

const formatHistoryText = (historyItems: string[], deviceType: DeviceType): string => {
  if (deviceType !== 'ipad') {
    return historyItems.join('\n');
  }

  const lines: string[] = [];
  for (let i = 0; i < historyItems.length; i += 2) {
    const whiteMove = historyItems[i] ?? '';
    const blackMove = historyItems[i + 1];
    lines.push(blackMove ? whiteMove.padEnd(HISTORY_LEFT_COLUMN_WIDTH, ' ') + blackMove : whiteMove);
  }

  return lines.join('\n');
};

export const useSidebarController = ({ eventBus, gameName, historyFormatter }: SidebarControllerOptions): SidebarController => {
  const [mode, setMode] = useState<SidebarMode>('idle');
  const [historyItems, setHistoryItems] = useState<string[]>([]);

  const textareaRef = useRef<HTMLTextAreaElement | null>(null);
  const inviteJoinStartedRef = useRef(false);

  const deviceType = useWindowDeviceType();
  const gameSessionId = useMemo(() => getGameSessionIdFromUrl(), []);

  const historyText = useMemo(
    () => formatHistoryText(historyItems, deviceType),
    [historyItems, deviceType]
  );

  useAutoScroll(textareaRef, historyText);

  const appendHistory = useCallback(
    (payload: HistoryPayload) => {
      const formatted = historyFormatter(payload);
      if (!formatted) {
        return;
      }

      setHistoryItems((prev) => [...prev, formatted]);
    },
    [historyFormatter]
  );

  const connectWs = useCallback(
    (sessionId: string | null = null) => {
      const wsUrl = buildWebSocketUrl(gameName, sessionId);
      eventBus.emit(APP_EVENTS.wsConnect, wsUrl);
    },
    [eventBus, gameName]
  );

  const joinInviteGame = useCallback(() => {
    if (!gameSessionId || inviteJoinStartedRef.current) {
      return;
    }

    inviteJoinStartedRef.current = true;
    connectWs(gameSessionId);
    setHistoryItems([]);
    setMode('joining');
    eventBus.emit(APP_EVENTS.toast, { message: 'Подключение к игре...', type: 'info' });
  }, [connectWs, eventBus, gameSessionId]);

  const startGame = useCallback(() => {
    removeGameSessionIdFromUrl();
    connectWs(null);
    setHistoryItems([]);
    setMode('playing');
    eventBus.emit(APP_EVENTS.openColorPicker);
  }, [connectWs, eventBus]);

  const endGame = useCallback(() => {
    eventBus.emit(APP_EVENTS.showEndConfirm);
  }, [eventBus]);

  const goHome = useCallback(() => {
    eventBus.emit(APP_EVENTS.gameExit);
  }, [eventBus]);

  useEffect(() => {
    joinInviteGame();
  }, [joinInviteGame]);

  useEffect(() => {
    const subscription = merge(
      eventBus.observe(APP_EVENTS.openColorPicker).pipe(
        tap(() => {
          setMode('playing');
          eventBus.emit(APP_EVENTS.showColorPicker);
        })
      ),

      eventBus.observe(APP_EVENTS.wsState).pipe(
        tap(() => setMode('playing'))
      ),

      eventBus.observe(APP_EVENTS.wsServerError).pipe(
        tap(() => {
          setMode((currentMode) => currentMode === 'joining' ? 'finished' : currentMode);
        })
      ),

      eventBus.observe<HistoryPayload>(APP_EVENTS.gameEnded).pipe(
        tap((payload) => {
          removeGameSessionIdFromUrl();
          setMode('finished');
          appendHistory({ text: payload?.message ?? payload?.text });
        })
      ),

      eventBus.observe<HistoryPayload>(APP_EVENTS.addHistory).pipe(
        tap((payload) => {
          if (payload?.resetControls) {
            removeGameSessionIdFromUrl();
            setMode('finished');
            eventBus.emit(APP_EVENTS.wsDisconnect);
          }

          appendHistory(payload);
        })
      )
    ).subscribe();

    return () => subscription.unsubscribe();
  }, [appendHistory, eventBus]);

  return { mode, historyText, textareaRef, startGame, endGame, goHome,};
};