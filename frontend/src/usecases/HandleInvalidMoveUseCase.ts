import { GameState } from "../game/GameState";
import { BoardController } from "../board/BoardController";
import { EventBus } from "../EventBus";

export class HandleInvalidMoveUseCase {
  constructor(
    private state: GameState,
    private board: BoardController,
    private bus: EventBus
  ) {}

  execute(data: any) {
    const moves = data.availableMoves ?? [];
    const text = moves.length
      ? `Некорректный ход. Доступные: ${moves.join(", ")}`
      : "Некорректный ход. Повторите попытку";

		this.bus.emit("TOAST", { message: text, type: "error" });
    this.state.resetSelection();
    this.board.clearHighlights();
  }
}