type Events = {
  SOCKET_MESSAGE: any
  CELL_CLICK: string
  TOAST: { message: string; type?: "info" | "error" | "success" }
  OPEN_PROMOTION_DIALOG: { pieces: string[]; color: string }
  PROMOTE: { piece: string }
}

export class EventBus {
  private listeners: {
    [K in keyof Events]?: ((data: Events[K]) => void)[]
  } = {}

  on<K extends keyof Events>(event: K, cb: (data: Events[K]) => void) {
    (this.listeners[event] ??= []).push(cb)
  }

  emit<K extends keyof Events>(event: K, data: Events[K]) {
    this.listeners[event]?.forEach(cb => cb(data))
  }

  off<K extends keyof Events>(event: K, cb: (data: Events[K]) => void) {
    this.listeners[event] = this.listeners[event]?.filter(x => x !== cb)
  }
}