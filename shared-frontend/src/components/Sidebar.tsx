import React, { useState, useEffect, useRef, useMemo } from 'react';
import { EventBus } from '../game/EventBus';
import { useEvent } from '../game/useEvent';

export type HistoryFormatter = (payload: any) => string;

const defaultHistoryFormatter: HistoryFormatter = (payload) => {
  if (payload?.text) {
    return String(payload.text);
  }
  return '';
};

interface SidebarProps {
  eventBus: EventBus;
  historyFormatter?: HistoryFormatter;
}

export const Sidebar: React.FC<SidebarProps> = ({ eventBus, historyFormatter = defaultHistoryFormatter }) => {
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [historyItems, setHistoryItems] = useState<string[]>([]);
  const [deviceType, setDeviceType] = useState<'desktop' | 'ipad' | 'mobile'>('desktop');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    const handleResize = () => {
      const w = window.innerWidth;
      if (w <= 600) setDeviceType('mobile');
      else if (w <= 900) setDeviceType('ipad');
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
      textareaRef.current.scrollTop = textareaRef.current.scrollHeight;
    }
  }, [historyText]);

  const appendHistory = (payload: any) => {
    const formatted = historyFormatter(payload);
    if (formatted) {
      setHistoryItems((prev) => [...prev, formatted]);
    }
  };

  useEvent(eventBus, 'OPEN_COLOR_PICKER', () => {
    setIsPlaying(true);
    eventBus.emit('SHOW_COLOR_PICKER');
  });

  useEvent(eventBus, ['END_GAME', 'CONFETTI'], (payload: any) => {
    setIsPlaying(false);
    eventBus.emit('TOAST', payload);
    appendHistory({ text: payload?.message ?? payload?.text });
  });

  useEvent(eventBus, 'ADD_HISTORY', (payload: any) => {
    if (payload?.resetControls) {
      setIsPlaying(false);
    }
    appendHistory(payload);
  });

  const handleStart = () => {
    setHistoryItems([]);
    eventBus.emit('OPEN_COLOR_PICKER');
  };

  const handleEnd = () => {
    eventBus.emit('SHOW_END_CONFIRM');
  };

  const handleHome = () => {
    eventBus.emit('GAME_EXIT');
  };

  return (
    <>
      {!isPlaying ? (
        <>
          <button className="btn btn-start" data-start onClick={handleStart}>
            Начать игру
          </button>
          <button className="btn btn-home" data-home onClick={handleHome}>
            На главную страницу
          </button>
        </>
      ) : (
        <button className="btn btn-end" data-end onClick={handleEnd}>
          Завершить игру
        </button>
      )}

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