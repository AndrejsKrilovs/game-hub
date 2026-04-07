import { GameState } from "../game/GameState";
import { GameSocket } from "../game/GameSocket";
import { EventBus } from "../EventBus";

export class SelectCellUseCase {
  constructor(
    private state: GameState,
    private socket: GameSocket,
    private bus: EventBus
  ) {}

  execute(coord: string) {
    const piece = this.state.getPiece(coord);

    if (!piece) {
      this.bus.emit("TOAST", { message: "Клетка пустая", type: "error" });
      return;
    }
    if (piece.color !== this.state.currentTurn) {
      const turn = this.state.currentTurn === "WHITE" ? "белые" : "чёрные";
      this.bus.emit("TOAST", { message: `Сейчас ход ${turn}`, type: "error" });
      return;
    }

    this.state.selected = coord;
    this.socket.send("GET_MOVES", { from: coord });
  }
}