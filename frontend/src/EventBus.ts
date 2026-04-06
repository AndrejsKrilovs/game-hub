type Handler = (payload?: any) => void;

export class EventBus {
  private listeners: Record<string, Handler[]> = {};

  on(event: string, handler: Handler) {
    (this.listeners[event] ||= []).push(handler);
  }

  emit(event: string, payload?: any) {
    this.listeners[event]?.forEach(h => h(payload));
  }

  off(event: string, handler: Handler) {
    this.listeners[event] =
      this.listeners[event]?.filter(h => h !== handler) || [];
  }
}