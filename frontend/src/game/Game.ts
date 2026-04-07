import {
  StartGameUseCase,
  EndGameUseCase,
  SelectCellUseCase,
  MakeMoveUseCase,
  HandleStateUseCase,
  HandleMovesUseCase,
  HandleInvalidMoveUseCase,
  HandleErrorUseCase
} from "../usecases";

import { GameState } from "./GameState";
import { GameSocket } from "./GameSocket";
import { GameUI } from "./GameUI";
import { BoardController } from "../board/BoardController";
import { EventBus } from "../EventBus";

export class Game {
  private state: GameState;
  private ui: GameUI;
  private socket: GameSocket;
  private board: BoardController;

  private startUC: StartGameUseCase;
  private endUC: EndGameUseCase;
  private selectUC: SelectCellUseCase;
  private moveUC: MakeMoveUseCase;
  private stateUC: HandleStateUseCase;
  private movesUC: HandleMovesUseCase;
  private invalidUC: HandleInvalidMoveUseCase;
  private errorUC: HandleErrorUseCase;

  constructor(private bus: EventBus) {
    this.state = new GameState();
    this.ui = new GameUI(bus);
    this.socket = new GameSocket(bus);
    this.board = new BoardController(bus);

    this.startUC = new StartGameUseCase(this.state, this.ui, this.socket, this.board, bus);
    this.endUC = new EndGameUseCase(this.state, this.ui, this.socket, this.board, bus);
    this.selectUC = new SelectCellUseCase(this.state, this.socket, bus);
    this.moveUC = new MakeMoveUseCase(this.state, this.socket, this.board, bus);
    this.stateUC = new HandleStateUseCase(
      this.state,
      this.board,
      this.ui,
      bus,
      (msg) => this.endUC.execute(msg),
      this.getSymbol.bind(this)
    );
    this.movesUC = new HandleMovesUseCase(this.state, this.board, bus);
    this.invalidUC = new HandleInvalidMoveUseCase(this.state, this.board, bus);
    this.errorUC = new HandleErrorUseCase(this.state, this.board, bus);

    this.ui.init(
      () => this.bus.emit("START_GAME"),
      () => this.bus.emit("END_GAME")
    );

    this.registerEvents();
  }

  private registerEvents() {
    this.bus.on("START_GAME", () => this.startUC.execute());
    this.bus.on("END_GAME", () => this.endUC.execute("Игра завершена"));
    this.bus.on("CELL_CLICK", (coord: string) => this.onCellClick(coord));
    this.bus.on("SOCKET_MESSAGE", (data: any) => this.handleSocket(data));
  }

  private handleSocket(data: any) {
    switch (data.type) {
      case "INIT":
      case "STATE":
        this.stateUC.execute(data);
        break;
      case "MOVES":
        this.movesUC.execute(data);
        break;
      case "INVALID_MOVE":
        this.invalidUC.execute(data);
        break;
      case "ERROR":
        this.errorUC.execute(data);
        break;
    }
  }

  private onCellClick(coord: string) {
    if (!this.state.isStarted) {
      this.bus.emit("TOAST", {
        message: "Сначала начните игру",
        type: "error"
      });
      return;
    }

    if (!this.state.selected) {
      this.selectUC.execute(coord);
    }
		else {
      this.moveUC.execute(coord);
    }
  }

  private getSymbol(type: string, color: string): string {
    const map: any = {
      Pawn: { WHITE: "♙", BLACK: "♟" },
      Rook: { WHITE: "♖", BLACK: "♜" },
      Knight: { WHITE: "♘", BLACK: "♞" },
      Bishop: { WHITE: "♗", BLACK: "♝" },
      Queen: { WHITE: "♕", BLACK: "♛" },
      King: { WHITE: "♔", BLACK: "♚" }
    };

    return map[type]?.[color] || "?";
  }
}