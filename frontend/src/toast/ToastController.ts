import { colorComponent } from "./ColorComponent"
import { endGameComponent } from "./EndGameComponent"

class ToastController {
  control = (eventBus: EventBus, root: HTMLElement) => {
    let color: string | null = null
    let currentComponent: "color" | "end" | null = null

    const render = (component: { init: (el: HTMLElement) => void }) => {
      root.innerHTML = ""
      component.init(root)
      root.classList.add("show")
    }

    eventBus.on("SHOW_COLOR_PICKER", () => {
      currentComponent = "color"
      render(colorComponent)
    })
    eventBus.on("SHOW_END_CONFIRM", () => {
      currentComponent = "end"
      render(endGameComponent)
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
          root.classList.remove("show")
          root.innerHTML = ""
          eventBus.emit("START_GAME", { color })
        }
      }
      if (currentComponent === "end") {
        if (target.matches("[data-yes]")) {
          root.classList.remove("show")
          root.innerHTML = ""
          color = null
          eventBus.emit("END_GAME")
        }
        if (target.matches("[data-no]")) {
					root.innerHTML = ""
          root.classList.remove("show")
        }
      }
    })
  }
}

export const toastController = new ToastController()