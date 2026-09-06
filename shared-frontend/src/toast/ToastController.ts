import { colorComponent } from "./ColorComponent"
import { endGameComponent } from "./EndGameComponent"
import { messageComponent } from "./MessageComponent"
import type { EventBus } from "../game/EventBus"

type ToastType = "info" | "success" | "error"
type CurrentComponent = "color" | "end" | "message" | null

class ToastController {
  private static instance?: ToastController

  private constructor() {}

  static getInstance = (): ToastController => {
    if (!ToastController.instance) {
      ToastController.instance = new ToastController()
    }

    return ToastController.instance
  }

  control = (eventBus: EventBus, root: HTMLElement) => {
    let color: string | null = null
    let currentComponent: CurrentComponent = null
    let clearTimer: number | undefined

    const clear = () => {
      root.classList.remove("show", "info", "success", "error")
      root.innerHTML = ""
      color = null
      currentComponent = null

      if (clearTimer) {
        window.clearTimeout(clearTimer)
        clearTimer = undefined
      }
    }

    const render = (
      component: { init: (el: HTMLElement, data?: any) => void },
      data?: any,
      type: ToastType = "info"
    ) => {
      root.classList.remove("info", "success", "error")
      component.init(root, data)
      root.classList.add(type, "show")
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
    eventBus.on("TOAST", (payload: any) => {
      currentComponent = "message"
      render(messageComponent, payload?.message ?? "", payload?.type ?? "info")
      clearTimer = window.setTimeout(clear, 2000)
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
    })
  }
}

export const toastController = ToastController.getInstance()