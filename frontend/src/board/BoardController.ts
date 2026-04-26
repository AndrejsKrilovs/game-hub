import { boardComponent } from "./BoardComponent"
import { pieceComponent } from "./PieceComponent"

class BoardController {
  control = (eventBus: EventBus, root: HTMLElement) => {
    let board: HTMLElement | null = null

    eventBus.on("WS:STATE", (payload) => {
      boardComponent.init(root)
      board = root.querySelector(".board")
      if (!board) return
      pieceComponent.init(board, payload.pieces)
    })
    eventBus.on("GAME_ENDED", () => {
      root.innerHTML = ""
      board = null
    })
  }
}

export const boardController = new BoardController()