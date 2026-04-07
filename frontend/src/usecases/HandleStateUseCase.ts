import { GameState } from "../game/GameState";
import { GameUI } from "../game/GameUI";
import { BoardController } from "../board/BoardController";
import { EventBus } from "../EventBus";

export class HandleStateUseCase {
  constructor(
    private state: GameState,
    private board: BoardController,
    private ui: GameUI,
    private bus: EventBus,
    private endGame: (msg: string) => void,
    private getSymbol: (t: string, c: string) => string
  ) {}

  execute(data: any) {
    this.state.currentTurn = data.turn;
    this.state.pieces = data.pieces;
    this.board.render(data.pieces, this.getSymbol);

    if (this.state.pendingMove) {
      const { piece, from, to } = this.state.pendingMove;
      this.ui.addToHistory(piece, from, to);
      this.state.pendingMove = null;
    }

    this.state.resetSelection();
    this.board.clearHighlights();

    switch (data.state) {
      case "CHECK":
        this.bus.emit("TOAST", { message: "ШАХ!", type: "info" });
        break;
      case "CHECKMATE":
        this.endGame("МАТ! Игра завершена.");
        break;
      case "STALEMATE":
        this.endGame("ПАТ! Нету доступных ходов, игра завершена.");
        break;
    }
  }
}