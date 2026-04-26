import { boardComponent } from "./BoardComponent"

const pieceMap: Record<string, Record<string, string>> = {
  Pawn: { WHITE: "♙", BLACK: "♟" },
  Rook: { WHITE: "♖", BLACK: "♜" },
  Knight: { WHITE: "♘", BLACK: "♞" },
  Bishop: { WHITE: "♗", BLACK: "♝" },
  Queen: { WHITE: "♕", BLACK: "♛" },
  King: { WHITE: "♔", BLACK: "♚" }
}

const getSymbol = (type: string, color: string): string => pieceMap[type]?.[color] ?? "?"

class BoardController {
  control = (eventBus: EventBus, root: HTMLElement) => {
    const renderPieces = (board: HTMLElement, pieces: any[]) => {
      board.querySelectorAll(".cell").forEach(cell => {
        const coord = cell.querySelector(".coord")
        cell.innerHTML = ""
        if (coord) cell.appendChild(coord)
      })

      pieces.forEach(p => {
        const coord = `${p.coordinates.file}${p.coordinates.rank}`
        const cell = board.querySelector(`[data-pos="${coord}"]`)
        if (!cell) return
        cell.textContent = getSymbol(p.type, p.color)
      })
    }

		eventBus.on("GAME_ENDED", () => root.innerHTML = "")
		eventBus.on("WS:STATE", (payload) => {
      boardComponent.init(root)
      renderPieces(root.querySelector(".board")!, payload.pieces)
    })
  }
}

export const boardController = new BoardController()