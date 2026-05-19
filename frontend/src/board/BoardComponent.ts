import type { BoardPerspective } from "./BoardTypes"

class BoardComponent {
  init = (root: HTMLElement, perspective: BoardPerspective = "WHITE") => {
    const files = perspective === "WHITE"
      ? ["a", "b", "c", "d", "e", "f", "g", "h"]
      : ["h", "g", "f", "e", "d", "c", "b", "a"]

    const ranks = perspective === "WHITE"
      ? [8, 7, 6, 5, 4, 3, 2, 1]
      : [1, 2, 3, 4, 5, 6, 7, 8]

    const renderCells = () => {
      let html = ""

      for (let row = 0; row < 8; row++) {
        for (let col = 0; col < 8; col++) {
          const isLight = (row + col) % 2 === 0
          const file = files[col]
          const rank = ranks[row]
          const coord = `${file}${rank}`

          html += `
            <div class="cell ${isLight ? "light" : "dark"}" data-pos="${coord}">
              ${row === 7 ? `<span class="coord bottom">${file}</span>` : ""}
              ${col === 0 ? `<span class="coord left">${rank}</span>` : ""}
            </div>
          `
        }
      }

      return html
    }

    root.innerHTML = `<div class="board">${renderCells()}</div>`
  }
}

export const boardComponent = new BoardComponent()