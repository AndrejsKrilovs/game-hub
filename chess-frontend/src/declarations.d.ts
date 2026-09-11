declare module 'shared-frontend' {
  import type { UserConfig } from 'vite';

  export const bootstrapGame: (
    gameName: 'chess' | 'checkers',
    historyFormatter?: (payload: any) => string
  ) => {
    eventBus: any;
    appContainer: HTMLElement;
    toastContainer: HTMLElement;
  };
  export const createGameConfig: (options: { base: string; title: string }) => UserConfig;
}