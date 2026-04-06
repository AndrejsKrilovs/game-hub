export class GameSocket {
  private ws!: WebSocket;

  constructor(private onMessage: (data: any) => void) {}

  connect() {
    this.ws = new WebSocket("ws://localhost:8080/ws");

    this.ws.onmessage = ({ data }) => {
      this.onMessage(JSON.parse(data));
    };
  }

  send(type: string, payload: any) {
    this.ws.send(JSON.stringify({ type, ...payload }));
  }

  close() {
    this.ws?.close();
  }
}