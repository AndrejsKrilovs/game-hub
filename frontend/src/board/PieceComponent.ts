import type { PieceType, PieceName } from "./BoardTypes"

class PieceComponent {
  init = (root: HTMLElement, pieces?: PieceType[]) => {
		const getSymbol = (type: PieceName, color: "WHITE" | "BLACK"): string => ({
      Pawn: { WHITE: "♙", BLACK: "♟" },
      Rook: { WHITE: "♖", BLACK: "♜" },
      Knight: { WHITE: "♘", BLACK: "♞" },
      Bishop: { WHITE: "♗", BLACK: "♝" },
      Queen: { WHITE: "♕", BLACK: "♛" },
      King: { WHITE: "♔", BLACK: "♚" }
    }[type]?.[color] ?? "?")

    root.querySelectorAll(".cell").forEach(cell => {
      const coord = cell.querySelector(".coord")
      cell.innerHTML = ""
      if (coord) cell.appendChild(coord)
    })

    pieces.forEach(p => {
      const coord = `${p.coordinates.file}${p.coordinates.rank}`
      const cell = root.querySelector(`[data-pos="${coord}"]`)
      if (!cell) return
      cell.innerHTML = `<span data-piece="${p.color}_${p.type}">${getSymbol(p.type, p.color)}</span>`
    })
  }
}

export const pieceComponent = new PieceComponent()