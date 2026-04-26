class BoardComponent {
  init = (root: HTMLElement) => {
		const files = ["a","b","c","d","e","f","g","h"]

		const renderCells = () => {
      let html = ""

      for (let row = 0; row < 8; row++) {
        for (let col = 0; col < 8; col++) {
          const isLight = (row + col) % 2 === 0
          const coord = `${files[col]}${8 - row}`

          html += `
            <div class="cell ${isLight ? "light" : "dark"}" data-pos="${coord}">
              ${row === 7  ? `<span class="coord bottom">${files[col]}</span>` : ""}
              ${col === 0  ? `<span class="coord left">${8 - row}</span>` : ""}
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