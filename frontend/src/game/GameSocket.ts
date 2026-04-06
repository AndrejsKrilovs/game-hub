import { EventBus } from "../EventBus";

export class GameSocket {
  private ws!: WebSocket;

  constructor(private bus: EventBus) {}

  connect() {
    this.ws = new WebSocket("ws://localhost:8080/ws");

    this.ws.onmessage = ({ data }) => {
      this.bus.emit("SOCKET_MESSAGE", JSON.parse(data));
    };
  }

  send(type: string, payload: any) {
    this.ws.send(JSON.stringify({ type, ...payload }));
  }

  close() {
    this.ws?.close();
  }
}