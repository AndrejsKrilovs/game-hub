export const APP_EVENTS = {
  wsConnect: 'WS_CONNECT',
  wsDisconnect: 'WS_DISCONNECT',
  wsSend: 'WS_SEND',
  wsOpen: 'WS_OPEN',
  wsClose: 'WS_CLOSE',
  wsError: 'WS_ERROR',
  wsMessage: 'WS_MESSAGE',

  wsState: 'WS:STATE',
  wsServerError: 'WS:ERROR',

  openColorPicker: 'OPEN_COLOR_PICKER',
  showColorPicker: 'SHOW_COLOR_PICKER',
  showEndConfirm: 'SHOW_END_CONFIRM',

  startGame: 'START_GAME',
  invite: 'INVITE',
  endGame: 'END_GAME',
  gameEnded: 'GAME_ENDED',
  gameExit: 'GAME_EXIT',

  addHistory: 'ADD_HISTORY',
  toast: 'TOAST',
  confetti: 'CONFETTI',
} as const;