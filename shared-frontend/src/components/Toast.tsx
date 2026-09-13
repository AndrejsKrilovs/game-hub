import React, { useState, useEffect } from 'react';
import { EventBus } from '../game/EventBus';
import { useEvent } from '../game/useEvent';

export type ToastType = 'info' | 'success' | 'error';
type ColorType = 'WHITE' | 'BLACK';

type ToastState = | { kind: 'color' } | { kind: 'end' } | { kind: 'message'; text: string; type: ToastType } | null;

interface ToastProps {
  eventBus: EventBus;
}

export const Toast: React.FC<ToastProps> = ({ eventBus }) => {
  const [toast, setToast] = useState<ToastState>(null);
  const [selectedColor, setSelectedColor] = useState<ColorType | null>(null);

  useEffect(() => {
    if (toast?.kind === 'message') {
      const timer = setTimeout(() => setToast(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [toast]);

  useEvent(eventBus, 'SHOW_COLOR_PICKER', () => {
    setSelectedColor(null);
    setToast({ kind: 'color' });
  });

  useEvent(eventBus, 'SHOW_END_CONFIRM', () => {
    setToast({ kind: 'end' });
  });

  useEvent(eventBus, 'TOAST', (payload: any) => {
    setToast({
      kind: 'message',
      text: payload?.message ?? '',
      type: payload?.type ?? 'info',
    });
  });

  if (!toast) {
    return null;
  }

  const handleStartGame = () => {
    if (!selectedColor) return;
    eventBus.emit('START_GAME', { color: selectedColor });
    setToast(null);
  };

  const handleConfirmEnd = () => {
    setToast(null);
    eventBus.emit('END_GAME');
  };

  const handleClose = () => {
    setToast(null);
  };

  const typeClass = toast.kind === 'message' ? toast.type : 'info';

  return (
    <div className={`toast ${typeClass} show`}>
      {toast.kind === 'color' && (
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

      {toast.kind === 'end' && (
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

      {toast.kind === 'message' && (
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