declare module 'shared-frontend' {
  import type { UserConfig } from 'vite';

  export type HistoryFormatter = (payload: any) => string;

  export class EventBus {
    on(event: string, callback: (...args: any[]) => void): void;
    emit(event: string, ...args: any[]): void;
  }

  export const bootstrapGame: (
    gameName: 'chess' | 'checkers',
    historyFormatter?: HistoryFormatter
  ) => {
    eventBus: EventBus;
    appContainer: HTMLElement;
    toastContainer: HTMLElement;
  };

  export const createGameConfig: (options: { base: string; title: string }) => UserConfig;
}