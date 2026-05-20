import { colorComponent } from "./ColorComponent"
import { endGameComponent } from "./EndGameComponent"
import { messageComponent } from "./MessageComponent"
import { promotionComponent } from "./PromotionComponent"

type MoveType = {
  from: string
  to: string
  piece: string
  color: string
}

class ToastController {
  control = (eventBus: EventBus, root: HTMLElement) => {
    let color: string | null = null
    let promotionMove: MoveType | null = null
    let currentComponent: "color" | "end" | "message" | "promotion" | null = null

    const clear = () => {
      root.classList.remove("show", "info", "success", "error")
      root.innerHTML = ""
      color = null
      promotionMove = null
      currentComponent = null
    }

    const render = (component: { init: (el: HTMLElement, msg?: string) => void }, data?: any) => {
      component.init(root, data)
			data ? root.classList.add("info", "show") : root.classList.add("show")
    }

    eventBus.on("SHOW_COLOR_PICKER", () => {
      currentComponent = "color"
      color = null
      render(colorComponent)
    })
    eventBus.on("SHOW_END_CONFIRM", () => {
      currentComponent = "end"
      render(endGameComponent)
    })
    eventBus.on("TOAST", ({ message }) => {
      currentComponent = "message"
      render(messageComponent, message)
      setTimeout(clear, 2000)
    })
		eventBus.on("WS:PROMOTION", ({ move, availablePieces }) => {
			promotionMove = move
			currentComponent = "promotion"
			render(promotionComponent, { color: move.color, availablePieces })
		})

    root.addEventListener("click", (e) => {
      const target = e.target as HTMLElement

      if (currentComponent === "color") {
        if (target.matches("[data-color]")) {
          color = target.dataset.color || null
					root.querySelectorAll(".btn").forEach(b => b.classList.remove("btn-selected"))
          target.classList.add("btn-selected")
        }
        if (target.matches("[data-start]")) {
          if (!color) return
          eventBus.emit("START_GAME", { color })
          clear()
        }
      }
      if (currentComponent === "end") {
        if (target.matches("[data-yes]")) {
          clear()
          eventBus.emit("END_GAME")
        }
        if (target.matches("[data-no]")) {
          clear()
        }
      }
			if (currentComponent === "promotion") {
				promotionMove.piece = target.dataset.piece
				eventBus.emit("WS_SEND", { type: "PROMOTE", payload: promotionMove })
				clear()
      }
    })
  }
}

export const toastController = new ToastController()