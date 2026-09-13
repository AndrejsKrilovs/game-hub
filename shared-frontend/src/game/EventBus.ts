type Handler = (payload?: unknown) => void

export class EventBus {
  private static instance?: EventBus
  private listeners: Record<string, Handler[]> = {}

  private constructor() {}

  static getInstance = (): EventBus => {
    if (!EventBus.instance) {
      EventBus.instance = new EventBus()
    }

    return EventBus.instance
  }

  emit = (event: string, payload?: unknown) =>
    this.listeners[event]?.forEach(h => h(payload))

  off = (event: string, handler: Handler) => {
    this.listeners[event] = this.listeners[event]?.filter(h => h !== handler) || []
  }

  on = (event: string, handler: Handler) => {
    (this.listeners[event] ||= []).push(handler)
    return () => this.off(event, handler)
  }
}

export const eventBus = EventBus.getInstance()