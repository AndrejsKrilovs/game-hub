import { boardComponent } from "./BoardComponent"
import { pieceComponent } from "./PieceComponent"

class BoardController {
  control = (eventBus: EventBus, root: HTMLElement) => {
    let board: HTMLElement | null = null
    let turnColor: "WHITE" | "BLACK" | null = null

    root.addEventListener("click", (e) => {
      const target = e.target as HTMLElement
      const cell = target.closest<HTMLElement>(".cell")
			if (!cell) return
      eventBus.emit("CELL_CLICK", { cord: cell.dataset.pos })
    })

    eventBus.on("WS:STATE", (payload) => {
      boardComponent.init(root)
      board = root.querySelector(".board")
      if (!board) return

      turnColor = payload.turn
      pieceComponent.init(board, payload.pieces)
    })
    eventBus.on("GAME_ENDED", () => {
      root.innerHTML = ""
      board = null
      turnColor = null
    })
		eventBus.on("HIGHLIGHT_MOVES", (moves) => {
      root.querySelectorAll(".cell.highlight").forEach(c => c.classList.remove("highlight"))
      moves.forEach(pos => root.querySelector(`[data-pos="${pos}"]`)?.classList.add("highlight"))
    })
		eventBus.on("CLEAR_HIGHLIGHTS", () =>
      root.querySelectorAll(".cell").forEach(c => c.classList.remove("highlight"))
    )
  }
}

export const boardController = new BoardController()