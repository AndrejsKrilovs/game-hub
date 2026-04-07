import { GameState } from "../game/GameState";
import { GameSocket } from "../game/GameSocket";
import { GameUI } from "../game/GameUI";
import { BoardController } from "../board/BoardController";
import { EventBus } from "../EventBus";

export class StartGameUseCase {
  constructor(
    private state: GameState,
    private ui: GameUI,
    private socket: GameSocket,
    private board: BoardController,
    private bus: EventBus
  ) {}

  execute() {
    if (this.state.isStarted) return;
    this.state.isStarted = true;
    this.state.gameOver = false;
    this.ui.setStarted(true);
    this.ui.clearHistory();
    this.board.create();
    this.socket.connect();
    this.bus.emit("TOAST", { message: "Игра началась", type: "success" });
  }
}