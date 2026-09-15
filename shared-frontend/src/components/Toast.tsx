import React, { useState, useEffect } from 'react';
import { EventBus } from '../game/EventBus';
import { useEvent } from '../game/useEvent';

export type ToastType = 'info' | 'success' | 'error';
type ColorType = 'WHITE' | 'BLACK';

type ToastState =
  | { kind: 'color'; isManualPlayer: boolean }
  | { kind: 'end' }
  | { kind: 'game_over'; text: string }
  | { kind: 'message'; text: string; type: ToastType }
  | null;

interface ToastProps {
  eventBus: EventBus;
}

export const Toast: React.FC<ToastProps> = ({ eventBus }) => {
  const [toast, setToast] = useState<ToastState>(null);
  const [selectedColor, setSelectedColor] = useState<ColorType | null>(null);

  useEffect(() => {
    const { kind } = toast || {};

    if (kind === 'message') {
      const timer = setTimeout(() => setToast(null), 3000);
      return () => clearTimeout(timer);
    }
    if (kind === 'game_over') {
      const timer = setTimeout(() => {
        setToast(null);
        eventBus.emit('WS_DISCONNECT');
      }, 2000);
      return () => clearTimeout(timer);
    }
  }, [toast, eventBus]);

  useEvent(eventBus, 'SHOW_COLOR_PICKER', ({ isManualPlayer }: any = {}) => {
    setSelectedColor(null);
    setToast({ kind: 'color', isManualPlayer: Boolean(isManualPlayer) });
  });

  useEvent(eventBus, 'SHOW_END_CONFIRM', () => {
    setToast({ kind: 'end' });
  });

  useEvent(eventBus, 'GAME_ENDED', ({ message, text }: any = {}) => {
    setToast({ kind: 'game_over', text: message ?? text });
  });

  useEvent(eventBus, 'TOAST', ({ message = '', type = 'info' }: any = {}) => {
    setToast({ kind: 'message', text: message, type });
  });

  if (!toast) return null;
  const { kind } = toast;

  const handleStartGame = () => {
    if (!selectedColor || toast.kind !== 'color') return;
    const { isManualPlayer } = toast;
    const eventName = isManualPlayer ? 'SEND_PLAYER_COLOR' : 'START_GAME';
    eventBus.emit(eventName, { color: selectedColor });
    setToast(null);
  };

  const handleConfirmEnd = () => {
    setToast(null);
    eventBus.emit('END_GAME');
  };

  const handleClose = () => {
    setToast(null);
  };

  const typeClass = kind === 'message' ? toast.type : 'info';

  return (
    <div className={`toast ${typeClass} show`}>
      {kind === 'color' && (
        <div className="toast-content">
          <div>Выберите цвет фигур</div>
          <div className="toast-actions">
            <button
              className={`btn btn-end ${selectedColor === 'WHITE' ? 'btn-selected' : ''}`}
              onClick={() => setSelectedColor('WHITE')}
            >
              Белые
            </button>
            <button
              className={`btn btn-end ${selectedColor === 'BLACK' ? 'btn-selected' : ''}`}
              onClick={() => setSelectedColor('BLACK')}
            >
              Чёрные
            </button>
          </div>
          <button className="btn btn-start" onClick={handleStartGame}>
            Старт
          </button>
        </div>
      )}

      {kind === 'end' && (
        <div className="toast-content">
          <div>Завершить игру досрочно?</div>
          <div className="toast-actions">
            <button className="btn btn-end" onClick={handleConfirmEnd}>
              Да
            </button>
            <button className="btn btn-end" onClick={handleClose}>
              Нет
            </button>
          </div>
        </div>
      )}

      {kind === 'game_over' && (
        <div className="toast-content">
          <div className="toast-message">
            <div className="toast-text">{toast.text}</div>
          </div>
        </div>
      )}

      {kind === 'message' && (
        <div className="toast-content">
          <div className="toast-message">
            <span>ℹ️</span>
            <div className="toast-text">{toast.text}</div>
          </div>
        </div>
      )}
    </div>
  );
};