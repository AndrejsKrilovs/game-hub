import { GameState } from "./GameState";
import { GameSocket } from "./GameSocket";
import { GameUI } from "./GameUI";
import { BoardController } from "../board/BoardController";
import { EventBus } from "../EventBus";

export class Game {
  private state = new GameState();
  private ui: GameUI;
  private socket: GameSocket;
  private board: BoardController;

  constructor(private bus: EventBus) {
    this.ui = new GameUI(bus);
    this.socket = new GameSocket(bus);
    this.board = new BoardController(bus);

    this.ui.init(this.startGame.bind(this), this.endGame.bind(this));

    bus.on("CELL_CLICK", this.onCellClick.bind(this));
    bus.on("SOCKET_MESSAGE", this.handleMessage.bind(this));
  }

  private startGame() {
    if (this.state.isStarted) return;

    this.state.isStarted = true;
    this.state.gameOver = false;

    this.ui.setStarted(true);
    this.ui.clearHistory();

    this.board.create();
    this.socket.connect();

    this.bus.emit("TOAST", { message: "Игра началась", type: "success" });
  }

  private endGame() {
    this.finishGame("Игра завершена");
  }

  private finishGame(msg: string) {
    this.state.isStarted = false;
    this.state.gameOver = true;

    this.ui.setStarted(false);
    this.socket.close();
    this.board.destroy();

    this.bus.emit("TOAST", { message: msg, type: "info" });
  }

  private handleMessage(data: any) {
    switch (data.type) {
      case "INIT":
      case "STATE":
        this.handleState(data);
        break;
      case "MOVES":
        this.handleMoves(data);
        break;
      case "INVALID_MOVE":
        this.handleInvalidMove(data);
        break;
      case "ERROR":
        this.handleError(data);
        break;
    }
  }

  private handleState(data: any) {
    this.state.currentTurn = data.turn;
    this.state.pieces = data.pieces;

    if (this.state.pendingMove) {
      this.bus.emit("MOVE_DONE", this.state.pendingMove);
      this.state.pendingMove = null;
    }

    this.state.resetSelection();
    this.board.clearHighlights();
    this.board.render(data.pieces, this.getSymbol);

    this.bus.emit("STATE_UPDATED", data);

    switch (data.state) {
      case "CHECK":
        this.bus.emit("TOAST", { message: "ШАХ!", type: "info" });
        break;
      case "CHECKMATE":
        this.finishGame("МАТ! Начать новую игру?");
        break;
      case "STALEMATE":
        this.finishGame("ПАТ! Начать новую игру?");
        break;
    }
  }

  private handleMoves(data: any) {
    if (!this.state.selected) return;

    if (!data.moves?.length) {
      this.bus.emit("TOAST", {
        message: `Фигура на ${this.state.selected} не имеет ходов`,
        type: "info"
      });

      this.state.resetSelection();
      this.board.clearHighlights();
      return;
    }

    this.state.availableMoves = data.moves;
    this.board.highlight(data.moves);
  }

  private handleInvalidMove(data: any) {
    this.state.pendingMove = null;

    this.bus.emit("TOAST", {
      message: "Недопустимый ход. Повторите попытку",
      type: "error"
    });

    this.state.resetSelection();
    this.board.clearHighlights();
  }

  private handleError(data: any) {
    this.state.resetSelection();
    this.board.clearHighlights();

    this.bus.emit("TOAST", {
      message: data.message ?? "Ошибка",
      type: "error"
    });
  }

  private onCellClick(coord: string) {
    if (!this.state.isStarted) {
      this.bus.emit("TOAST", { message: "Сначала начните игру", type: "error" });
      return;
    }

    if (this.state.gameOver) {
      this.bus.emit("TOAST", { message: "Игра завершена", type: "error" });
      return;
    }

    if (!this.state.selected) {
      const piece = this.getPiece(coord);

      if (!piece) {
        this.bus.emit("TOAST", { message: "Клетка пустая", type: "error" });
        return;
      }

      if (piece.color !== this.state.currentTurn) {
        const turn = this.state.currentTurn === "WHITE" ? "белых" : "чёрных";
        this.bus.emit("TOAST", { message: `Сейчас ход ${turn}`, type: "error" });
        return;
      }

      this.state.selected = coord;
      this.socket.send("GET_MOVES", { from: coord });
      return;
    }

    if (coord === this.state.selected) {
      this.state.resetSelection();
      this.board.clearHighlights();
      return;
    }

    if (!this.state.availableMoves.includes(coord)) {
      this.bus.emit("TOAST", {
        message: "Недопустимый ход. Повторите попытку",
        type: "error"
      });

      this.state.resetSelection();
      this.board.clearHighlights();
      return;
    }

    this.socket.send("MOVE", {
      from: this.state.selected,
      to: coord
    });

    this.state.pendingMove = {
      piece: this.getPiece(this.state.selected)!,
      from: this.state.selected,
      to: coord
    };
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

	private getPiece(coord: string) {
    return this.state.pieces.find(
      p => `${p.coordinates.file}${p.coordinates.rank}` === coord
    );
  }
}