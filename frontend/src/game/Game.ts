import { GameState } from "./GameState";
import { GameUI } from "./GameUI";
import { GameSocket } from "./GameSocket";
import { Toast } from "./Toast";
import { BoardController } from "../board/BoardController";

type Color = "WHITE" | "BLACK";

export class Game {
  private state;
  private ui: GameUI;
  private socket: GameSocket;
  private board: BoardController;
  private toast: Toast;

  constructor(private bus: EventBus) {
    this.state = new GameState();
    this.ui = new GameUI(bus);
    this.socket = new GameSocket(bus);
    this.board = new BoardController(bus);
    this.toast = new Toast(bus);
    this.ui.init(this.openStartDialog, this.openEndDialog);
    bus.on("CELL_CLICK", this.onCellClick);
    bus.on("SOCKET_MESSAGE", this.handleMessage);
  }

  private openStartDialog = (): void => {
    this.bus.emit("OPEN_COLOR_PICKER", this.startGame);
  };

  private startGame = (color: Color): void => {
    if (this.state.isStarted) return;
    this.state.isStarted = true;
    this.state.gameOver = false;
    this.ui.setStarted(true);
    this.ui.clearHistory();

    this.board.create();
    this.socket.connect();
    this.bus.on("SOCKET_OPEN", () => {
      this.socket.send("START_GAME", { color });
    });
  };

  private openEndDialog = (): void => {
    this.bus.emit("CONFIRM", {
      message: "Завершить игру?",
      onConfirm: this.endGame
    });
  };

  private endGame = (): void => {
    this.finishGame("Игра завершена");
  };

  private finishGame = (msg: string): void => {
    this.state.isStarted = false;
    this.state.gameOver = true;
    this.ui.setStarted(false);
    this.socket.close();
    this.bus.emit("TOAST", { message: msg });
  };

  private handleMessage = (data: any): void => {
    switch (data.type) {
      case "INIT":
      case "STATE":
        this.handleState(data);
        break;
      case "MOVE":
        this.handleMoveEvent(data);
        break;
      case "MOVES":
        this.handleMoves(data);
        break;
      case "INVALID_MOVE":
        this.handleInvalidMove();
        break;
      case "ERROR":
        this.handleError(data);
        break;
    }
  };

  private handleState = (data: any): void => {
    this.state.currentTurn = data.turn;
    this.state.pieces = data.pieces;
    this.state.resetSelection();
    this.board.clearHighlights();
    this.board.render(data.pieces, this.getSymbol);

    switch (data.state) {
      case "CHECK":
        this.bus.emit("TOAST", { message: "ШАХ!" });
        break;
      case "CHECKMATE":
        this.finishGame("МАТ!");
        break;
      case "STALEMATE":
        this.finishGame("ПАТ!");
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
      this.bus.emit("TOAST", { message: `Нет доступных ходов` });
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

  private onCellClick = (coord: string): void => {
    if (!this.state.isStarted) {
      this.bus.emit("TOAST", { message: "Сначала начните игру" });
      return;
    }

    if (!this.state.selected) {
      const piece = this.getPiece(coord);
      if (!piece) return;
      if (piece.color !== this.state.currentTurn) return;

      this.state.selected = coord;
      this.socket.send("GET_MOVES", { from: coord });
      return;
    }

    if (!this.state.availableMoves.includes(coord)) {
      this.state.resetSelection();
      this.board.clearHighlights();
      return;
    }

    this.socket.send("MOVE", {
      from: this.state.selected,
      to: coord
    });
  };

  private getSymbol = (type: string, color: string): string => {
    const map: Record<string, Record<string, string>> = {
      Pawn: { WHITE: "♙", BLACK: "♟" },
      Rook: { WHITE: "♖", BLACK: "♜" },
      Knight: { WHITE: "♘", BLACK: "♞" },
      Bishop: { WHITE: "♗", BLACK: "♝" },
      Queen: { WHITE: "♕", BLACK: "♛" },
      King: { WHITE: "♔", BLACK: "♚" }
    };

    return map[type]?.[color] ?? "?";
  };

  private getPiece = (coord: string) =>
    this.state.pieces.find(
      p => `${p.coordinates.file}${p.coordinates.rank}` === coord
    );
}