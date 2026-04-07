import { BoardView } from "./BoardView";
import { EventBus } from "../EventBus";

export class BoardController {
  private board: BoardView | null = null;

  constructor(private bus: EventBus) {}

  create() {
    const app = document.getElementById("app")!;
    app.innerHTML = "";
    this.board = new BoardView(this.bus);
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
      this.board?.setPiece(pos, getSymbol(p.type, p.color));
    });
  }

  highlight(moves: string[]) {
    this.board?.highlightMoves(moves);
  }

  clearHighlights() {
    this.board?.clearHighlights();
  }
}