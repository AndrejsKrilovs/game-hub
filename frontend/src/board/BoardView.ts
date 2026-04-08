const files = ["a","b","c","d","e","f","g","h"];

export class BoardView {
    private onCellClick: (coord: string) => void;
    private element: HTMLElement;

    constructor(onCellClick: (coord: string) => void) {
        this.onCellClick = onCellClick;
        this.element = this.createBoard();
    }

    get(): HTMLElement {
        return this.element;
    }

    render(container: HTMLElement) {
        container.appendChild(this.element);
    }

		highlightMoves(moves: string[]) {
        moves.forEach(pos => {
            const cell = this.element.querySelector(`[data-pos="${pos}"]`);
            cell?.classList.add("highlight");
        });
    }

		clearHighlights() {
        this.element.querySelectorAll(".cell").forEach(c => c.classList.remove("highlight"));
    }

    private createBoard(): HTMLElement {
        const board = document.createElement("div");
        board.className = "board";

        for (let row = 0; row < 8; row++) {
            for (let col = 0; col < 8; col++) {

                const cell = document.createElement("div");
                const isLight = (row + col) % 2 === 0;
                cell.className = `cell ${isLight ? "light" : "dark"}`;

                const coord = `${files[col]}${8 - row}`;
                cell.dataset.pos = coord;

                if (row === 7) {
                    const label = document.createElement("span");
                    label.className = "coord bottom";
                    label.textContent = files[col];
                    cell.appendChild(label);
                }
                if (col === 0) {
                    const label = document.createElement("span");
                    label.className = "coord left";
                    label.textContent = String(8 - row);
                    cell.appendChild(label);
                }

                cell.onclick = () => this.onCellClick(coord);
                board.appendChild(cell);
            }
        }

        return board;
    }

		setPiece(coord: string, symbol: string) {
      const cell = this.element.querySelector(`[data-pos="${coord}"]`);
      if (!cell) return;

      cell.textContent = "";
      cell.appendChild(document.createTextNode(symbol));
    }

    clear() {
      this.element.querySelectorAll(".cell").forEach(c => {
          if (!c.querySelector(".coord")) {
              c.textContent = "";
          }
      });
    }
}