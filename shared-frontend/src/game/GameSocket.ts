import type { EventBus } from "./EventBus"

type WSMessage = {
  type: string
  payload?: unknown
}

export class GameSocket {
  private ws?: WebSocket
  private initialized = false
  private reconnectTimer?: number
  private url?: string
  private shouldReconnect = true

  constructor(private bus: EventBus, private game: string) {
    this.bus.on("WS_CONNECT", (url) => {
      if (typeof url === "string") {
        this.shouldReconnect = true
        this.connect(url)
      }
    })

    this.bus.on("WS_DISCONNECT", () => {
      this.shouldReconnect = false
      this.stopReconnect()
      this.close()
    })

    this.bus.on("GAME_EXIT", async () => {
      this.shouldReconnect = false
      this.stopReconnect()
      this.close()

      await fetch(`/games/${this.game}/exit`, {
        method: "POST",
        credentials: "same-origin"
      })

      window.location.replace("/")
    })
  }

  close = () => {
    if (!this.ws) return

    this.ws.onclose = null
    this.ws.close()
    this.ws = undefined
    this.bus.emit("WS_CLOSE")
  }

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
      if (this.shouldReconnect) {
        this.scheduleReconnect()
      }
    }
    this.ws.onerror = (event) => {
      this.bus.emit("WS_ERROR", event)
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
    if (!this.url || !this.shouldReconnect) return

    this.stopReconnect()
    console.warn("Переподключение через 2 секунды...")

    this.reconnectTimer = window.setTimeout(() => {
      if (this.shouldReconnect && this.url) {
        this.connect(this.url)
      }
    }, 2000)
  }

  private stopReconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = undefined
    }
  }
}

export const gameSocket = (bus: EventBus, game: string) => new GameSocket(bus, game)