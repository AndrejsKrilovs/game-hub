import { EventBus } from "../game/EventBus";

export type HistoryFormatter = (payload: any) => string

const defaultHistoryFormatter: HistoryFormatter = (payload) => {
  if (payload?.text) {
    return String(payload.text)
  }

  return ""
}

class SidebarController {
  private static instance?: SidebarController
  private historyFormatter: HistoryFormatter = defaultHistoryFormatter

  private constructor() {}

  static getInstance = (): SidebarController => {
    if (!SidebarController.instance) {
      SidebarController.instance = new SidebarController()
    }

    return SidebarController.instance
  }

  control = (
    eventBus: EventBus,
    root: HTMLElement
  ) => {
    const homeBtn = root.querySelector<HTMLButtonElement>("[data-home]")!
    const startBtn = root.querySelector<HTMLButtonElement>("[data-start]")!
    const historyEl = root.querySelector<HTMLTextAreaElement>("textarea")!
    const endBtn = root.querySelector<HTMLButtonElement>("[data-end]")!

    const append = (text: string) => {
      historyEl.value += text + "\n"
      historyEl.scrollTop = historyEl.scrollHeight
    }

    root.addEventListener("click", (e) => {
      const target = e.target as HTMLElement

      if (target.matches("[data-start]")) {
        eventBus.emit("OPEN_COLOR_PICKER")
        historyEl.value = ""
      }
      if (target.matches("[data-end]")) {
        eventBus.emit("SHOW_END_CONFIRM")
      }
      if (target.matches("[data-home]")) {
        eventBus.emit("GAME_EXIT")
      }
    })

    eventBus.on("OPEN_COLOR_PICKER", () => {
      homeBtn.classList.add("hidden")
      startBtn.classList.add("hidden")
      endBtn.classList.remove("hidden")
      eventBus.emit("SHOW_COLOR_PICKER")
    })
    eventBus.on("GAME_ENDED", (payload: any) => {
      homeBtn.classList.remove("hidden")
      startBtn.classList.remove("hidden")
      endBtn.classList.add("hidden")
      eventBus.emit("TOAST", payload)
      eventBus.emit("ADD_HISTORY", { text: payload.message })
    })
    eventBus.on("ADD_HISTORY", (payload: any) => {
      if (payload.resetControls) {
        homeBtn.classList.remove("hidden")
        startBtn.classList.remove("hidden")
        endBtn.classList.add("hidden")
      }

      append(this.historyFormatter(payload))
    })
  }

  setHistoryFormatter = (historyFormatter: HistoryFormatter) => {
    this.historyFormatter = historyFormatter
  }
}

export const sidebarController = SidebarController.getInstance()