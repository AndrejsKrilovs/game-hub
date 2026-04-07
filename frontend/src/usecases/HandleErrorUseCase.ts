import { GameState } from "../game/GameState";
import { BoardController } from "../board/BoardController";
import { EventBus } from "../EventBus";

export class HandleErrorUseCase {
  constructor(
    private state: GameState,
    private board: BoardController,
    private bus: EventBus
  ) {}

  execute(data: any) {
    this.state.resetSelection();
    this.board.clearHighlights();
    this.bus.emit("TOAST", { message: data.message ?? "Ошибка", type: "error" });
  }
}