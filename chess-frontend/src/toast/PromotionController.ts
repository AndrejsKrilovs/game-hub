import type { EventBus } from "shared-frontend"
import { promotionComponent } from "./PromotionComponent"

type MoveType = {
  from: string
  to: string
  piece: string
  color: string
}

class PromotionController {
  control = (eventBus: EventBus, root: HTMLElement) => {
    let promotionMove: MoveType | null = null
    let isOpen = false

    const clear = () => {
      root.classList.remove("show", "info", "success", "error")
      root.innerHTML = ""
      promotionMove = null
      isOpen = false
    }

    eventBus.on("WS:PROMOTION", ({ move, availablePieces }: any) => {
      promotionMove = move
      isOpen = true

      promotionComponent.init(root, {
        color: move.color,
        availablePieces
      })

      root.classList.add("info", "show")
    })

    root.addEventListener("click", (e) => {
      if (!isOpen || !promotionMove) return

      const target = e.target as HTMLElement

      if (!target.matches("[data-piece]")) {
        return
      }

      const piece = target.dataset.piece

      if (!piece) {
        return
      }

      promotionMove.piece = piece

      eventBus.emit("WS_SEND", {
        type: "PROMOTE",
        payload: promotionMove
      })

      clear()
    })
  }
}

export const promotionController = new PromotionController()