import React from 'react';
import type { EventBus } from '../../game/EventBus';

import {
  defaultHistoryFormatter,
  useSidebarController,
  type HistoryFormatter,
  type SidebarMode,
} from './useSidebarController';

export type { HistoryFormatter } from './useSidebarController';

interface SidebarProps {
  eventBus: EventBus;
  gameName: string;
  historyFormatter?: HistoryFormatter;
}

interface SidebarActionsProps {
  mode: SidebarMode;
  onStart: () => void;
  onEnd: () => void;
  onHome: () => void;
}

interface HistoryProps {
  value: string;
  textareaRef: React.RefObject<HTMLTextAreaElement | null>;
}

const SidebarActions: React.FC<SidebarActionsProps> = ({ mode, onStart, onEnd, onHome }) => {
  if (mode === 'joining') {
    return null;
  }
  if (mode === 'playing') {
    return (
      <button className="btn btn-end" data-end onClick={onEnd}>
        Завершить игру
      </button>
    );
  }

  return (
    <>
      <button className="btn btn-start" data-start onClick={onStart}>
        Начать игру
      </button>
      <button className="btn btn-home" data-home onClick={onHome}>
        На главную страницу
      </button>
    </>
  );
};

const History: React.FC<HistoryProps> = ({ value, textareaRef }) => {
  return (
    <div className="history">
      <label htmlFor="game-history">История ходов</label>

      <textarea
        id="game-history"
        ref={textareaRef}
        value={value}
        readOnly
      />
    </div>
  );
};

export const Sidebar: React.FC<SidebarProps> = ({ eventBus, gameName, historyFormatter = defaultHistoryFormatter }) => {
  const sidebar = useSidebarController({ eventBus, gameName, historyFormatter });

  return (
    <>
      <SidebarActions
        mode={sidebar.mode}
        onStart={sidebar.startGame}
        onEnd={sidebar.endGame}
        onHome={sidebar.goHome}
      />

      <History
        value={sidebar.historyText}
        textareaRef={sidebar.textareaRef}
      />
    </>
  );
};