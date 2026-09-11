import { boardComponent } from "./BoardComponent"
import { pieceComponent } from "./PieceComponent"
import type { BoardPerspective } from "./BoardTypes"
import type { EventBus } from "shared-frontend"

type MoveHighlight = { to: string }

class BoardController {
  control = (eventBus: EventBus, root: HTMLElement) => {
    let perspective: BoardPerspective = "WHITE"

    root.addEventListener("click", (e) => {
      const target = e.target as HTMLElement
      const cell = target.closest<HTMLElement>(".cell")
			if (!cell) return
      eventBus.emit("CELL_CLICK", { cord: cell.dataset.pos })
    })

    eventBus.on("START_GAME", ({ color }: { color: BoardPerspective }) => { perspective = color})
    eventBus.on("UPDATE_BOARD", ({ pieces }: { pieces: any }) => {
      boardComponent.init(root, perspective)
      const boardElement = root.querySelector<HTMLElement>(".board")
      if (boardElement) pieceComponent.init(boardElement, pieces)
    })
    eventBus.on("HIGHLIGHT_MOVES", (moves: MoveHighlight[]) => {
      root.querySelectorAll(".cell.highlight").forEach(c => c.classList.remove("highlight"))
      moves.forEach(pos => root.querySelector(`[data-pos="${pos.to}"]`)?.classList.add("highlight"))
    })
		eventBus.on("CLEAR_HIGHLIGHTS", () =>
      root.querySelectorAll(".cell").forEach(c => c.classList.remove("highlight"))
    )
		eventBus.on("GAME_ENDED", () => {
			const board = root.querySelector(".board")
			board?.classList.add("finished")
    })
  }
}

export const boardController = new BoardController()