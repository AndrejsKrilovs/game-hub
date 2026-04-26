class GameController {
	control = (eventBus: EventBus) => {
    let selected: string | null = null

		eventBus.on("END_GAME", () => eventBus.emit("WS_SEND", { type: "END_GAME" }))
		eventBus.on("START_GAME", ({ color }) => eventBus.emit("WS_SEND", { type: "START_GAME", payload: { color } }))
    eventBus.on("CELL_CLICK", ({ cell, turnColor }) => {
      const cord = cell.dataset.pos
      if (!cord) return

      const piece = cell.querySelector<HTMLElement>("[data-piece]")?.dataset.piece
      if (!selected) {
        if (!piece || !piece.startsWith(turnColor)) {
          return eventBus.emit("TOAST", { message: piece ? "Фигура другого цвета" : "Пустая клетка" })
        }

        selected = cord
        return eventBus.emit("WS_SEND", { type: "GET_MOVES", payload: { from: cord } })
      }
      if (cord === selected) {
        selected = null
        return eventBus.emit("CLEAR_HIGHLIGHTS")
      }
      if (piece && piece.startsWith(turnColor)) {
        selected = cord
        return eventBus.emit("WS_SEND", { type: "GET_MOVES", payload: { from: cord } })
      }

      eventBus.emit("WS_SEND", { type: "MOVE", payload: { from: selected, to: cord } })
      eventBus.emit("CLEAR_HIGHLIGHTS")
      selected = null
    })

		eventBus.on("WS:GAME_ENDED", (payload) => eventBus.emit("GAME_ENDED", payload))
    eventBus.on("WS:MOVES", ({ moves }) => {
      if (!moves.length) {
        return eventBus.emit("TOAST", { message: "Нет доступных ходов" })
      }

      eventBus.emit("HIGHLIGHT_MOVES", moves)
    })
    eventBus.on("WS:INVALID_MOVE", ({ availableMoves }) =>
      eventBus.emit("TOAST", { message: `Некорректный ход.\nДоступно: ${availableMoves.join(", ")}` })
    )
	}
}

export const gameController = new GameController()