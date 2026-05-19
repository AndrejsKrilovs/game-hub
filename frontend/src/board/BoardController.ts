import { boardComponent } from "./BoardComponent"
import { pieceComponent } from "./PieceComponent"
import { BoardPerspective } from "./BoardTypes";

class BoardController {
  control = (eventBus: EventBus, root: HTMLElement) => {
    let perspective: BoardPerspective = "WHITE"

    root.addEventListener("click", (e) => {
      const target = e.target as HTMLElement
      const cell = target.closest<HTMLElement>(".cell")
			if (!cell) return
      eventBus.emit("CELL_CLICK", { cord: cell.dataset.pos })
    })

    eventBus.on("START_GAME", ({ color }) => {
      perspective = color as BoardPerspective
    })
    eventBus.on("UPDATE_BOARD", ({ pieces }) => {
      boardComponent.init(root, perspective)
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