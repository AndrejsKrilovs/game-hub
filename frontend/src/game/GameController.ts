class GameController {
	control = (eventBus: EventBus) => {
    let selectedCell: string | null = null

		eventBus.on("END_GAME", () => eventBus.emit("WS_SEND", { type: "END_GAME" }))
		eventBus.on("START_GAME", ({ color }) => eventBus.emit("WS_SEND", { type: "START_GAME", payload: { color } }))
    eventBus.on("CELL_CLICK", ({ cord }) => {
			if (!selectedCell) {
				selectedCell = cord
				return eventBus.emit("WS_SEND", { type: "GET_MOVES", payload: { from: selectedCell } })
			}
			if (cord === selectedCell) {
        selectedCell = null
        return eventBus.emit("CLEAR_HIGHLIGHTS")
      }

			eventBus.emit("CLEAR_HIGHLIGHTS")
      eventBus.emit("WS_SEND", { type: "MAKE_MOVE", payload: { from: selectedCell, to: cord } })
      selectedCell = null
    })

		eventBus.on("WS:GAME_ENDED", (payload) => eventBus.emit("GAME_ENDED", payload))
    eventBus.on("WS:MOVES", ({ moves }) => eventBus.emit("HIGHLIGHT_MOVES", moves))
    eventBus.on("WS:ERROR", ({ message }) => {
			selectedCell = null
			eventBus.emit("TOAST", { message })
    })
	}
}

export const gameController = new GameController()