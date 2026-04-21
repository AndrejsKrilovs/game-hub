import { GameState } from "./GameState";
import { GameUI } from "./GameUI";
import { GameSocket } from "./GameSocket";
import { Toast } from "./Toast";
import { getSymbol } from "./Util";
import { BoardController } from "../board/BoardController";

type Color = "WHITE" | "BLACK";

export class Game {
  private state;
  private ui: GameUI;
  private toast: Toast;
  private socket: GameSocket;
  private board: BoardController;

  constructor(private bus: EventBus) {
		this.state = new GameState();
    this.ui = new GameUI(bus);
    this.toast = new Toast(bus);
    this.socket = new GameSocket(bus);
    this.board = new BoardController(bus);
    this.ui.init(this.openStartDialog, this.openEndDialog);
    bus.on("CELL_CLICK", this.onCellClick);
    bus.on("SOCKET_MESSAGE", this.handleMessage);
  }

  private openStartDialog = (): void => {
    this.bus.emit("OPEN_COLOR_PICKER", (color: Color) => this.startGame(color));
  };

  private startGame = (color: Color): void => {
    if (this.state.isStarted) return;
    this.state.isStarted = true;
    this.state.gameOver = false;
    this.ui.setStarted(true);
    this.ui.clearHistory();

    this.board.create();
    this.socket.connect();
    const startHandler = () => {
      this.socket.send("START_GAME", { color });
      this.bus.off?.("SOCKET_OPEN", startHandler);
    };

    this.bus.on("SOCKET_OPEN", startHandler);
  };

  private openEndDialog = (): void => {
    this.bus.emit("CONFIRM", {
      message: "Завершить игру?",
      onConfirm: () => this.finishGame("Игра завершена", { destroyBoard: true })
    });
  };

  private finishGame = (msg: string, options?: { destroyBoard?: boolean }): void => {
    this.state.isStarted = false;
    this.state.gameOver = true;
    this.socket.send("END_GAME");
    this.socket.close();

		this.ui.setStarted(false);
    if (options?.destroyBoard) {
      this.board.destroy();
    }

    this.bus.emit("TOAST", { message: msg });
  };

  private handleMessage = (data: string): void => {
    const handlers = {
      INIT: this.handleState,
      STATE: this.handleState,
      MOVE: this.handleMoveEvent,
      MOVES: this.handleMoves,
      INVALID_MOVE: this.handleInvalidMove,
      ERROR: this.handleError,
      PROMOTION: this.handlePromotion
    }

    handlers[data.type]?.(data)
  }

  private handleState = (data: any): void => {
    this.state.currentTurn = data.turn;
    this.state.pieces = data.pieces;
    this.state.resetSelection();
    this.board.clearHighlights();
    this.board.render(data.pieces, getSymbol);

    switch (data.state) {
      case "CHECK":
        this.bus.emit("TOAST", { message: "ШАХ!" });
        break;
      case "CHECKMATE":
        this.finishGame("МАТ!");
        break;
      case "STALEMATE":
        this.finishGame("Ничья, доступных ходов больше нет");
        break;
    }
  };

  private handleMoveEvent = (data: any): void => {
    const { piece, from, to, color, isCastling, castlingType } = data;
    this.ui.addToHistory({ type: piece, color }, from, to, { isCastling, castlingType });
  };

  private handleMoves = (data: any): void => {
    if (!this.state.selected) return;

    if (!data.moves?.length) {
      this.bus.emit("TOAST", { message: "Нет доступных ходов" });
      this.state.resetSelection();
      this.board.clearHighlights();
      return;
    }

    this.state.availableMoves = data.moves;
    this.board.highlight(data.moves);
  };

  private handleInvalidMove = (): void => {
    this.bus.emit("TOAST", { message: "Недопустимый ход" });
    this.state.resetSelection();
    this.board.clearHighlights();
  };

  private handleError = (data: any): void => {
    this.state.resetSelection();
    this.board.clearHighlights();
    this.bus.emit("TOAST", { message: data.message ?? "Ошибка" });
  };

	private handlePromotion = (data: any): void => {
    this.state.promotion = data
    this.state.resetSelection()
    this.board.clearHighlights()
    this.bus.emit("OPEN_PROMOTION_DIALOG", data)
  }

  private onCellClick = (coord: string): void => {
    if (!this.state.isStarted) {
      return this.toastMsg("Сначала начните игру")
    }
    if (!this.state.selected) {
      const piece = this.getPiece(coord)
      if (!piece || piece.color !== this.state.currentTurn) return
      this.state.selected = coord
      return this.socket.send("GET_MOVES", { from: coord })
    }
    if (!this.state.availableMoves.includes(coord)) {
      this.resetSelection()
      return
    }

    this.socket.send("MOVE", { from: this.state.selected, to: coord })
  }

  private getPiece = (coord: string) => this.state.pieces.find(p => `${p.coordinates.file}${p.coordinates.rank}` === coord);
	private toastMsg = (message: string) => this.bus.emit("TOAST", { message })

  private resetSelection = () => {
    this.state.resetSelection()
    this.board.clearHighlights()
  }
}