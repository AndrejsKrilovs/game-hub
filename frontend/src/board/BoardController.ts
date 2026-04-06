import { BoardView } from "./boardView";

export class BoardController {
  private board: BoardView | null = null;

  constructor(private onCellClick: (coord: string) => void) {}

  create() {
    const app = document.getElementById("app")!;
    app.innerHTML = "";

    this.board = new BoardView(this.onCellClick);
    this.board.render(app);
  }

  destroy() {
    document.getElementById("app")!.innerHTML = "";
    this.board = null;
  }

  render(pieces: any[], getSymbol: any) {
    if (!this.board) return;

    this.board.clear();
    this.board.clearHighlights();

    pieces.forEach(p => {
      const pos = `${p.coordinates.file}${p.coordinates.rank}`;
      this.board!.setPiece(pos, getSymbol(p.type, p.color));
    });
  }

  highlight(moves: string[]) {
    this.board?.highlightMoves(moves);
  }

  clearHighlights() {
    this.board?.clearHighlights();
  }
}