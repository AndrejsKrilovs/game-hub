type WSMessage = {
  type: string
  payload?: unknown
}

class GameSocket {
  private ws?: WebSocket
  private initialized = false
  private reconnectTimer?: number
  private url?: string

  constructor(private bus: EventBus) {
    this.bus.on("WS_CONNECT", (url) => {
      if (typeof url === "string") {
        this.connect(url)
      }
    })
    this.bus.on("WS_DISCONNECT", () => {
      this.stopReconnect()
      this.close()
    })
  }

	close = () => this.ws?.close()

  connect = (url: string) => {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      console.warn("Пользователь уже подключён")
      return
    }

    this.url = url
    this.ws = new WebSocket(url)

    this.ws.onopen = () => {
      this.bus.emit("WS_OPEN")
      this.stopReconnect()
    }
    this.ws.onclose = () => {
      this.bus.emit("WS_CLOSE")
      this.scheduleReconnect()
    }
    this.ws.onerror = (e) => {
      this.bus.emit("WS_ERROR", e)
    }
    this.ws.onmessage = ({ data }) => {
      try {
        const msg: WSMessage = JSON.parse(data)
        this.bus.emit("WS_MESSAGE", msg)
        this.bus.emit(`WS:${msg.type}`, msg.payload)
      }
			catch {
        console.warn("Некорректное сообщение для протокола:", data)
      }
    }

    if (!this.initialized) {
      this.bus.on("WS_SEND", (msg) => {
        const message = msg as WSMessage
        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
          console.warn("Веб сокет не доступен")
          return
        }

        this.ws.send(JSON.stringify(message))
      })

      this.initialized = true
    }
  }

  private scheduleReconnect() {
    if (!this.url) return
    console.warn("Переподключение через 2 секунды...")
    this.reconnectTimer = window.setTimeout(() => this.connect(this.url!), 2000)
  }

  private stopReconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = undefined
    }
  }
}

export const gameSocket = (bus: EventBus) => new GameSocket(bus)