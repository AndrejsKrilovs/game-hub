import { pieceMetadata } from "../board/PieceComponent"

class SidebarController {
  control = (eventBus: EventBus, root: HTMLElement) => {
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
    })

    eventBus.on("OPEN_COLOR_PICKER", () => {
      startBtn.classList.add("hidden")
      endBtn.classList.remove("hidden")
      eventBus.emit("SHOW_COLOR_PICKER")
    })
    eventBus.on("GAME_ENDED", (payload) => {
      startBtn.classList.remove("hidden")
      endBtn.classList.add("hidden")
      eventBus.emit("TOAST", payload)
    })
		eventBus.on("ADD_HISTORY", (payload) => {
			if (payload.text) {
				startBtn.classList.remove("hidden")
        endBtn.classList.add("hidden")
				return append(`${payload.text}`)
			}

			const pieceColor = payload.color === "WHITE" ? "Белые" : "Чёрные"
			if (payload.castlingType) {
        return append(`${pieceColor}: ${ payload.castlingType === "SHORT" ? "короткая рокировка" : "длинная рокировка" }`)
      }

			const pieceName = pieceMetadata[payload.piece]?.name ?? type
			const getStateText = (state?: string): string => ({
          CHECK: " (шах)",
          CHECKMATE: " (мат)"
      })[state ?? ""] ?? ""
			append(`${pieceColor}: ${pieceName} ${payload.from} → ${payload.to} ${getStateText(payload.state)}`)
    })
  }
}

export const sidebarController = new SidebarController()