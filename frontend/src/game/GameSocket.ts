export class GameSocket {
  private ws!: WebSocket;
  private isOpen = false;

  constructor(private bus: EventBus) {}

  connect() {
    this.ws = new WebSocket("ws://localhost:8080/ws");

    this.ws.onopen = () => {
      this.isOpen = true;
      this.bus.emit("SOCKET_OPEN");
    };

    this.ws.onmessage = ({ data }) => {
      this.bus.emit("SOCKET_MESSAGE", JSON.parse(data));
    };

    this.ws.onclose = () => {
      this.isOpen = false;
    };
  }

  send(type: string, payload: any) {
    if (!this.isOpen) {
      console.warn("WS not ready");
      return;
    }

    this.ws.send(JSON.stringify({ type, ...payload }));
  }

  close() {
    this.ws?.close();
  }
}