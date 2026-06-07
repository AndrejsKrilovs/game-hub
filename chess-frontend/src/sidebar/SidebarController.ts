import { pieceMetadata } from "../board/PieceComponent"

class SidebarController {
  control = (eventBus: EventBus, root: HTMLElement) => {
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
    eventBus.on("GAME_ENDED", (payload) => {
      homeBtn.classList.remove("hidden")
      startBtn.classList.remove("hidden")
      endBtn.classList.add("hidden")
      eventBus.emit("TOAST", payload)
      eventBus.emit("ADD_HISTORY", { text: payload.message })
    })
		eventBus.on("ADD_HISTORY", (payload) => {
			if (payload.text) {
				homeBtn.classList.remove("hidden")
				startBtn.classList.remove("hidden")
        endBtn.classList.add("hidden")
				return append(`${payload.text}`)
			}

			const pieceColor = payload.color === "WHITE" ? "Белые" : "Чёрные"
			if (payload.castlingType) {
        return append(`${pieceColor}: ${ payload.castlingType === "SHORT" ? "короткая рокировка" : "длинная рокировка" }`)
      }

      const pieceName = pieceMetadata[payload.piece]?.name ?? payload.piece
			const getStateText = (state?: string): string => ({
          CHECK: " (шах)",
          CHECKMATE: " (мат)"
      })[state ?? ""] ?? ""
			append(`${pieceColor}: ${pieceName} ${payload.from} → ${payload.to} ${getStateText(payload.state)}`)
    })
  }
}

export const sidebarController = new SidebarController()