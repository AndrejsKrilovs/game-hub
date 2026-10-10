import './style.css';
import './mobile.css';

import React, { type ReactNode } from 'react';
import { fromEvent, type Subscription, tap } from 'rxjs';
import { createRoot, type Root } from 'react-dom/client';

import { type EventBus, eventBus } from './game/EventBus';
import { gameSocket } from './game/GameSocket';
import { APP_EVENTS } from './game/events';
import { useBaseGameController } from './game/useBaseGameController';

import { Sidebar } from './components/sidebar/Sidebar';
import type { HistoryFormatter } from './components/sidebar/Sidebar';
import { Toast } from './components/toast/Toast';
import { Confetti } from './components/confetti/Confetti';

export type GameName = 'chess' | 'checkers' | 'tictactoe' | 'cards';

export interface BootstrapGameContext {
  eventBus: EventBus;
  gameName: GameName;
}

export interface BootstrapGameOptions {
  historyFormatter?: HistoryFormatter;
  renderGame?: (context: BootstrapGameContext) => ReactNode;
}

const initCap = (value: string): string => {
  const normalized = value.trim();

  if (!normalized) {
    return '';
  }

  return normalized.charAt(0).toUpperCase() + normalized.slice(1).toLowerCase();
};

const ensureElement = <T extends HTMLElement>(
  selector: string,
  resolve: () => T | null
): T => {
  const element = resolve();

  if (!element) {
    throw new Error(`${selector} not found`);
  }

  return element;
};

const createElement = <K extends keyof HTMLElementTagNameMap>(
  tagName: K,
  id: string
): HTMLElementTagNameMap[K] => {
  const element = document.createElement(tagName);
  element.id = id;
  return element;
};

const ensureBaseLayout = (): void => {
  if (!document.getElementById('layout')) {
    const layout = createElement('div', 'layout');
    const sidebar = createElement('div', 'sidebar');
    const app = createElement('div', 'app');

    layout.appendChild(sidebar);
    layout.appendChild(app);
    document.body.appendChild(layout);
  }

  if (!document.getElementById('shared-ui-overlay')) {
    document.body.appendChild(createElement('div', 'shared-ui-overlay'));
  }
};

const getCurrentGamePath = (gameName: GameName): string => {
  return `/${gameName}${window.location.search}`;
};

const pushNavigationLock = (gameName: GameName): void => {
  history.pushState(
    { page: `${gameName}-lock` },
    '',
    getCurrentGamePath(gameName)
  );
};

const lockBrowserNavigation = (gameName: GameName): Subscription => {
  history.replaceState({ page: gameName }, '', getCurrentGamePath(gameName));
  pushNavigationLock(gameName);

  return fromEvent<PopStateEvent>(window, 'popstate')
    .pipe(
      tap(() => {
        pushNavigationLock(gameName);

        eventBus.emit(APP_EVENTS.toast, {
          message: 'Нет возможности пользоваться навигацией',
          type: 'info',
        });
      })
    )
    .subscribe();
};

interface GameRootProps {
  context: BootstrapGameContext;
  options: BootstrapGameOptions;
}

const GameRoot: React.FC<GameRootProps> = ({ context, options }) => {
  useBaseGameController(context.eventBus);

  return <>{options.renderGame?.(context)}</>;
};

export const bootstrapGame = (
  gameName: GameName,
  options: BootstrapGameOptions = {}
) => {
  const { historyFormatter } = options;

  ensureBaseLayout();
  document.title = initCap(gameName);

  const sidebarEl = ensureElement('#sidebar', () =>
    document.getElementById('sidebar')
  );

  const overlayEl = ensureElement('#shared-ui-overlay', () =>
    document.getElementById('shared-ui-overlay')
  );

  const appContainer = ensureElement('#app', () =>
    document.querySelector<HTMLElement>('#app')
  );

  const socket = gameSocket(eventBus, gameName);
  const navigationSubscription = lockBrowserNavigation(gameName);

  const sidebarRoot = createRoot(sidebarEl);
  const overlayRoot = createRoot(overlayEl);
  const appRoot: Root = createRoot(appContainer);

  sidebarRoot.render(
    <Sidebar
      eventBus={eventBus}
      gameName={gameName}
      historyFormatter={historyFormatter}
    />
  );

  overlayRoot.render(
    <>
      <Toast eventBus={eventBus} />
      <Confetti eventBus={eventBus} />
    </>
  );

  appRoot.render(
    <GameRoot
      context={{
        eventBus,
        gameName,
      }}
      options={options}
    />
  );

  return {
    eventBus,
    appContainer,
    sidebarRoot,
    overlayRoot,
    appRoot,

    destroy: () => {
      navigationSubscription.unsubscribe();
      socket.destroy();

      sidebarRoot.unmount();
      overlayRoot.unmount();
      appRoot.unmount();
    },
  };
};