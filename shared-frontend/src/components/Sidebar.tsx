import React, { useState, useEffect, useRef } from 'react';
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

export const Sidebar: React.FC<SidebarProps> = ({ eventBus, historyFormatter = defaultHistoryFormatter}) => {
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [history, setHistory] = useState<string>('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.scrollTop = textareaRef.current.scrollHeight;
    }
  }, [history]);

  const appendHistory = (payload: any) => {
    const formatted = historyFormatter(payload);
    if (formatted) {
      setHistory((prev) => prev + formatted + '\n');
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
    setHistory('');
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
          value={history}
          readOnly
        />
      </div>
    </>
  );
};