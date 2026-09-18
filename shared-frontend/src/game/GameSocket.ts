import axios from 'axios';
import { filter, fromEvent, merge, Subscription, tap, timer } from 'rxjs';

import type { EventBus } from './EventBus';
import { APP_EVENTS } from './events';

type WSMessage = {
  type: string;
  payload?: unknown;
};

const RECONNECT_DELAY_MS = 2000;

const isNonBlankString = (value: unknown): value is string => {
  return typeof value === 'string' && value.trim().length > 0;
};

export class GameSocket {
  private ws?: WebSocket;
  private url?: string;
  private shouldReconnect = true;

  private busSubscription?: Subscription;
  private socketSubscription?: Subscription;
  private reconnectSubscription?: Subscription;

  constructor(private readonly bus: EventBus, private readonly game: string) {
    this.registerBusEvents();
  }

  connect = (url: string): void => {
    if (this.isSameActiveConnection(url)) {
      console.warn('Пользователь уже подключён');
      return;
    }

    this.close();
    this.url = url;
    this.shouldReconnect = true;

    const socket = new WebSocket(url);
    this.ws = socket;
    this.bindSocket(socket);
  };

  close = (): void => {
    this.stopReconnect();
    this.socketSubscription?.unsubscribe();
    this.socketSubscription = undefined;

    const socket = this.ws;
    if (!socket) {
      return;
    }
    if (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING) {
      socket.close();
    }

    this.ws = undefined;
    this.bus.emit(APP_EVENTS.wsClose);
  };

  destroy = (): void => {
    this.busSubscription?.unsubscribe();
    this.busSubscription = undefined;
    this.shouldReconnect = false;
    this.close();
  };

  private registerBusEvents = (): void => {
    this.busSubscription = merge(
      this.bus.observe<unknown>(APP_EVENTS.wsConnect).pipe(
        filter(isNonBlankString),
        tap((url) => this.connect(url))
      ),

      this.bus.observe(APP_EVENTS.wsDisconnect).pipe(
        tap(() => {
          this.shouldReconnect = false;
          this.close();
        })
      ),

      this.bus.observe<WSMessage>(APP_EVENTS.wsSend).pipe(
        tap((message) => this.send(message))
      ),

      this.bus.observe(APP_EVENTS.gameExit).pipe(
        tap(() => void this.exitGame())
      )
    ).subscribe();
  };

  private bindSocket = (socket: WebSocket): void => {
    this.socketSubscription?.unsubscribe();

    this.socketSubscription = merge(
      fromEvent(socket, 'open').pipe(
        tap(() => {
          if (this.ws !== socket) {
            return;
          }

          this.stopReconnect();
          this.bus.emit(APP_EVENTS.wsOpen);
        })
      ),

      fromEvent(socket, 'close').pipe(
        tap(() => {
          if (this.ws !== socket) {
            return;
          }

          this.ws = undefined;
          this.bus.emit(APP_EVENTS.wsClose);
          if (this.shouldReconnect) {
            this.scheduleReconnect();
          }
        })
      ),

      fromEvent<Event>(socket, 'error').pipe(
        tap((event) => this.bus.emit(APP_EVENTS.wsError, event))
      ),

      fromEvent<MessageEvent<string>>(socket, 'message').pipe(
        tap((event) => this.handleMessage(event.data))
      )
    ).subscribe();
  };

  private send = (message: WSMessage): void => {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn('Веб сокет не доступен');
      return;
    }

    this.ws.send(JSON.stringify(message));
  };

  private handleMessage = (data: string): void => {
    try {
      const message = JSON.parse(data) as WSMessage;
      this.bus.emit(APP_EVENTS.wsMessage, message);
      this.bus.emit(`WS:${message.type}`, message.payload);
    }
    catch {
      console.warn('Некорректное сообщение для протокола:', data);
    }
  };

  private exitGame = async (): Promise<void> => {
    this.shouldReconnect = false;
    this.close();
    axios
      .post(`/games/${this.game}/exit`, undefined, { withCredentials: true })
      .catch((error) => console.warn('Не удалось корректно выйти из игры', error))
      .finally(() => window.location.replace('/'));
  };

  private isSameActiveConnection = (url: string): boolean => {
    if (!this.ws || this.url !== url) {
      return false;
    }

    return (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING);
  };

  private scheduleReconnect = (): void => {
    if (!this.url || !this.shouldReconnect) {
      return;
    }

    this.stopReconnect();
    console.warn('Переподключение через 2 секунды...');
    this.reconnectSubscription = timer(RECONNECT_DELAY_MS).subscribe(() => {
      if (this.shouldReconnect && this.url) {
        this.connect(this.url);
      }
    });
  };

  private stopReconnect = (): void => {
    this.reconnectSubscription?.unsubscribe();
    this.reconnectSubscription = undefined;
  };
}

export const gameSocket = (bus: EventBus, game: string): GameSocket => {
  return new GameSocket(bus, game);
};