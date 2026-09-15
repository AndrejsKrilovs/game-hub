import React, { useState, useEffect, useRef, useMemo } from 'react';
import { EventBus } from '../game/EventBus';
import { useEvent } from '../game/useEvent';

export type HistoryFormatter = (payload: any) => string;

const defaultHistoryFormatter: HistoryFormatter = ({ text } = {}) => (text ? String(text) : '');

interface SidebarProps {
  eventBus: EventBus;
  gameName: string;
  historyFormatter?: HistoryFormatter;
}

export const Sidebar: React.FC<SidebarProps> = ({ eventBus, gameName, historyFormatter = defaultHistoryFormatter }) => {
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [historyItems, setHistoryItems] = useState<string[]>([]);
  const [deviceType, setDeviceType] = useState<'desktop' | 'ipad' | 'mobile'>('desktop');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    const handleResize = () => {
      const { innerWidth } = window;
      if (innerWidth <= 600) setDeviceType('mobile');
      else if (innerWidth <= 900) setDeviceType('ipad');
      else setDeviceType('desktop');
    };

    handleResize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // 2 колонки только для iPad Mini (ipad), 1 колонка для mobile и desktop
  const historyText = useMemo(() => {
    if (deviceType === 'ipad') {
      const LEFT_COLUMN_WIDTH = 30;
      const lines: string[] = [];

      for (let i = 0; i < historyItems.length; i += 2) {
        const whiteMove = historyItems[i];
        const blackMove = historyItems[i + 1];

        if (blackMove !== undefined) {
          lines.push(whiteMove.padEnd(LEFT_COLUMN_WIDTH, ' ') + blackMove);
        }
        else {
          lines.push(whiteMove);
        }
      }
      return lines.join('\n');
    }

    return historyItems.join('\n');
  }, [historyItems, deviceType]);

  useEffect(() => {
    if (textareaRef.current) {
      const { scrollHeight } = textareaRef.current;
      textareaRef.current.scrollTop = scrollHeight;
    }
  }, [historyText]);

  const appendHistory = (payload: any) => {
    const formatted = historyFormatter(payload);
    if (formatted) {
      setHistoryItems((prev) => [...prev, formatted]);
    }
  };

  const connectWs = () => {
    const { protocol, host } = window.location;
    const wsProtocol = protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${wsProtocol}//${host}/${gameName}/ws`;
    eventBus.emit('WS_CONNECT', wsUrl);
  };

  useEvent(eventBus, 'OPEN_COLOR_PICKER', ({ isManualPlayer }: any = {}) => {
    setIsPlaying(true);
    eventBus.emit('SHOW_COLOR_PICKER', { isManualPlayer });
  });

  useEvent(eventBus, 'GAME_ENDED', ({ message, text }: any = {}) => {
    setIsPlaying(false);
    appendHistory({ text: message ?? text });
  });

  useEvent(eventBus, 'ADD_HISTORY', (payload: any) => {
    const { resetControls } = payload || {};
    if (resetControls) {
      setIsPlaying(false);
      eventBus.emit('WS_DISCONNECT');
    }
    appendHistory(payload);
  });

  const handleStart = (isManualPlayer: boolean) => {
    connectWs();
    setHistoryItems([]);
    eventBus.emit('OPEN_COLOR_PICKER', { isManualPlayer });
  };

  const handleEnd = () => eventBus.emit('SHOW_END_CONFIRM');
  const handleHome = () => eventBus.emit('GAME_EXIT');

  return (
    <>
      <div className="sidebar-actions">
        {!isPlaying ? (
          <>
            <button className="btn btn-start" data-start onClick={() => handleStart(false)}>
              Начать игру
            </button>
            <button className="btn btn-challenge" data-challenge onClick={() => handleStart(true)}>
              Вызов другу
            </button>
            <button className="btn btn-home" data-home onClick={handleHome}>
              На главную
            </button>
          </>
        ) : (
          <button className="btn btn-end" data-end onClick={handleEnd}>
            Завершить игру
          </button>
        )}
      </div>

      <div className="history">
        <label htmlFor="game-history">История ходов</label>
        <textarea
          id="game-history"
          ref={textareaRef}
          value={historyText}
          readOnly
        />
      </div>
    </>
  );
};