import { boardComponent } from "./BoardComponent"
import { pieceComponent } from "./PieceComponent"

class BoardController {
  control = (eventBus: EventBus, root: HTMLElement) => {
    root.addEventListener("click", (e) => {
      const target = e.target as HTMLElement
      const cell = target.closest<HTMLElement>(".cell")
			if (!cell) return
      eventBus.emit("CELL_CLICK", { cord: cell.dataset.pos })
    })

    eventBus.on("UPDATE_BOARD", ({ turn, pieces }) => {
      boardComponent.init(root)
      pieceComponent.init(root.querySelector(".board"), pieces)
    })
		eventBus.on("HIGHLIGHT_MOVES", (moves) => {
      root.querySelectorAll(".cell.highlight").forEach(c => c.classList.remove("highlight"))
      moves.forEach(pos => root.querySelector(`[data-pos="${pos.to}"]`)?.classList.add("highlight"))
    })
		eventBus.on("CLEAR_HIGHLIGHTS", () =>
      root.querySelectorAll(".cell").forEach(c => c.classList.remove("highlight"))
    )
		eventBus.on("GAME_ENDED", () => {
			const board = root.querySelector(".board")
			board.classList.add("finished")
    })
  }
}

export const boardController = new BoardController()