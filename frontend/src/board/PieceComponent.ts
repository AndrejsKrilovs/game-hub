import type { PieceType, PieceName } from "./BoardTypes"

class PieceComponent {
  init = (root: HTMLElement, pieces?: PieceType[]) => {
    root.querySelectorAll(".cell").forEach(cell => {
      const coord = cell.querySelector(".coord")
      cell.innerHTML = ""
      if (coord) cell.appendChild(coord)
    })

    pieces.forEach(p => {
      const coord = `${p.coordinates}`
      const cell = root.querySelector(`[data-pos="${coord}"]`)
      if (!cell) return
      cell.innerHTML = `<span data-piece="${p.color}_${p.type}">${pieceMetadata[p.type]?.[p.color] ?? "?"}</span>`
    })
  }
}

export const pieceComponent = new PieceComponent()

export const pieceMetadata = {
  Pawn: { name: "пешка", WHITE: "♙", BLACK: "♟" },
  Rook: { name: "ладья", WHITE: "♖", BLACK: "♜" },
  Knight: { name: "конь", WHITE: "♘", BLACK: "♞" },
  Bishop: { name: "слон", WHITE: "♗", BLACK: "♝" },
  Queen: { name: "ферзь", WHITE: "♕", BLACK: "♛" },
  King: { name: "король", WHITE: "♔", BLACK: "♚" }
} as const