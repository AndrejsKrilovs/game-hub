import React from 'react';
import type { EventBus } from '../../game/EventBus';

import { useToastController, type ColorType,} from './useToastController';

export type { ToastType } from './useToastController';

interface ToastProps {
  eventBus: EventBus;
}

interface ColorButtonProps {
  color: ColorType;
  label: string;
  selectedColor: ColorType | null;
  onSelect: (color: ColorType) => void;
}

const ColorButton: React.FC<ColorButtonProps> = ({ color, label, selectedColor, onSelect }) =>
  (
    <button
      className={`btn btn-end ${selectedColor === color ? 'btn-selected' : ''}`}
      onClick={() => onSelect(color)}
    >
      {label}
    </button>
  );

export const Toast: React.FC<ToastProps> = ({ eventBus }) => {
  const toastController = useToastController({ eventBus });
  const { toast, selectedColor } = toastController;
  if (!toast) {
    return null;
  }

  return (
    <div className={`toast ${toast.kind === 'message' ? toast.type : 'info'} show`}>
      {toast.kind === 'color' && (
        <div className="toast-content">
          <div>Выберите цвет фигур</div>

          <div className="toast-actions">
            <ColorButton
              color="WHITE"
              label="Белые"
              selectedColor={selectedColor}
              onSelect={toastController.selectColor}
            />

            <ColorButton
              color="BLACK"
              label="Чёрные"
              selectedColor={selectedColor}
              onSelect={toastController.selectColor}
            />
          </div>

          <div className="toast-actions toast-actions-vertical">
            <button
              className="btn btn-start"
              onClick={toastController.startGame}
              disabled={!selectedColor}
            >
              Старт
            </button>
            <button
              className="btn btn-start"
              onClick={toastController.invite}
              disabled={!selectedColor}
            >
              Пригласить друга
            </button>
          </div>
        </div>
      )}

      {toast.kind === 'end' && (
        <div className="toast-content">
          <div>Завершить игру досрочно?</div>
          <div className="toast-actions">
            <button className="btn btn-end" onClick={toastController.confirmEnd}>
              Да
            </button>
            <button className="btn btn-end" onClick={toastController.close}>
              Нет
            </button>
          </div>
        </div>
      )}

      {toast.kind === 'game_over' && (
        <div className="toast-content">
          <div className="toast-message">
            <div className="toast-text">{toast.text}</div>
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