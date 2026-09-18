import './style.css';
import './mobile.css';

import { fromEvent, Subscription, tap } from 'rxjs';
import { createRoot } from 'react-dom/client';

import { eventBus } from './game/EventBus';
import { gameSocket } from './game/GameSocket';
import { APP_EVENTS } from './game/events';

import { Sidebar } from './components/sidebar/Sidebar';
import type { HistoryFormatter } from './components/sidebar/Sidebar';
import { Toast } from './components/toast/Toast';
import { Confetti } from './components/confetti/Confetti';

type GameName = 'chess' | 'checkers' | 'tictactoe';

const initCap = (value: string): string => {
  const normalized = value.trim();

  if (!normalized) {
    return '';
  }

  return normalized.charAt(0).toUpperCase() + normalized.slice(1).toLowerCase();
};

const ensureElement = <T extends HTMLElement>(selector: string, resolve: () => T | null): T => {
  const element = resolve();
  if (!element) {
    throw new Error(`${selector} not found`);
  }

  return element;
};

const createElement = <K extends keyof HTMLElementTagNameMap>(tagName: K, id: string): HTMLElementTagNameMap[K] => {
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
  history.pushState({ page: `${gameName}-lock` }, '', getCurrentGamePath(gameName));
};

const lockBrowserNavigation = (gameName: GameName): Subscription => {
  history.replaceState({ page: gameName }, '', getCurrentGamePath(gameName));
  pushNavigationLock(gameName);

  return fromEvent<PopStateEvent>(window, 'popstate')
    .pipe(
      tap(() => {
        pushNavigationLock(gameName);
        eventBus.emit(APP_EVENTS.toast, { message: 'Нет возможности пользоваться навигацией', type: 'info' });
      })
    )
    .subscribe();
};

export const bootstrapGame = (gameName: GameName, historyFormatter?: HistoryFormatter) => {
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

  return {
    eventBus, appContainer, sidebarRoot, overlayRoot, destroy: () => {
      navigationSubscription.unsubscribe();
      socket.destroy();
      sidebarRoot.unmount();
      overlayRoot.unmount();
    }
  };
};