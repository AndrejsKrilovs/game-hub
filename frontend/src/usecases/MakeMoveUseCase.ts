import { GameState } from "../game/GameState";
import { GameSocket } from "../game/GameSocket";
import { BoardController } from "../board/BoardController";
import { EventBus } from "../EventBus";

export class MakeMoveUseCase {
  constructor(
    private state: GameState,
    private socket: GameSocket,
    private board: BoardController,
    private bus: EventBus
  ) {}

  execute(coord: string) {
    if (!this.state.selected) return;
    if (coord === this.state.selected) {
      this.state.resetSelection();
      this.board.clearHighlights();
      return;
    }
    if (!this.state.availableMoves.includes(coord)) {
      this.bus.emit("TOAST", { message: "Недопустимый ход. Повторите попытку", type: "error" });
      this.state.resetSelection();
      this.board.clearHighlights();
      return;
    }

    this.socket.send("MOVE", {
      from: this.state.selected,
      to: coord
    });

    this.state.pendingMove = {
      piece: this.state.getPiece(this.state.selected),
      from: this.state.selected,
      to: coord
    };
  }
}