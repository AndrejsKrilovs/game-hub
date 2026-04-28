type Handler = (payload?: unknown) => void

class EventBus {
  private listeners: Record<string, Handler[]> = {}

  emit = (event: string, payload?: unknown) =>
    this.listeners[event]?.forEach(h => h(payload))

  off = (event: string, handler: Handler) =>
    this.listeners[event] = this.listeners[event]?.filter(h => h !== handler) || []

	on = (event: string, handler: Handler) => {
    (this.listeners[event] ||= []).push(handler)
    return () => this.off(event, handler)
  }
}

export const eventBus = new EventBus()