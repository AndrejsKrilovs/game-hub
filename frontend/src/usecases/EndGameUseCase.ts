import { GameState } from "../game/GameState";
import { GameSocket } from "../game/GameSocket";
import { GameUI } from "../game/GameUI";
import { BoardController } from "../board/BoardController";
import { EventBus } from "../EventBus";

export class EndGameUseCase {
  constructor(
    private state: GameState,
    private ui: GameUI,
    private socket: GameSocket,
    private board: BoardController,
    private bus: EventBus
  ) {}

  execute(msg: string) {
    this.state.isStarted = false;
    this.state.gameOver = true;
    this.ui.setStarted(false);
    this.socket.close();
    this.board.destroy();
    this.bus.emit("TOAST", { message: msg, type: "info" });
  }
}