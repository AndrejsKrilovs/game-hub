import { GameState } from "../game/GameState";
import { BoardController } from "../board/BoardController";
import { EventBus } from "../EventBus";

export class HandleMovesUseCase {
  constructor(
    private state: GameState,
    private board: BoardController,
    private bus: EventBus
  ) {}

  execute(data: any) {
    if (!this.state.selected) return;

    if (!data.moves?.length) {
      this.bus.emit("TOAST", { message: `Фигура на ${this.state.selected} не имеет ходов`, type: "info" });
      this.state.resetSelection();
      this.board.clearHighlights();
      return;
    }

    this.state.availableMoves = data.moves;
    this.board.highlight(data.moves);
  }
}