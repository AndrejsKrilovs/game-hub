import { GameState } from "./GameState";
import { GameSocket } from "./GameSocket";
import { GameUI } from "./GameUI";
import { BoardController } from "../board/BoardController";
import { Toast } from "./Toast";

export class Game {
  private state = new GameState();
  private ui = new GameUI();
  private socket = new GameSocket(this.handleMessage.bind(this));
  private board = new BoardController(this.onCellClick.bind(this));

  constructor() {
    this.ui.init(this.startGame.bind(this), this.endGame.bind(this));
  }

  // ===== lifecycle =====

  private startGame() {
    if (this.state.isStarted) return;

    this.state.isStarted = true;
    this.state.gameOver = false;

    this.ui.setStarted(true);
    this.ui.clearHistory();

    this.board.create();
    this.socket.connect();

    Toast.show("Игра началась", "success");
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

    Toast.show(msg, "info");
  }

  // ===== socket =====

  private handleMessage(data: any) {
    switch (data.type) {
      case "INIT":
      case "STATE":
        this.handleState(data);
        break;

      case "MOVES":
        this.board.highlight(data.moves);
        break;
    }
  }

  private handleState(data: any) {
    this.state.currentTurn = data.turn;
    this.state.pieces = data.pieces;

    this.board.render(data.pieces, this.getSymbol);

    switch (data.state) {
      case "CHECK":
        Toast.show("ШАХ!");
        break;

      case "CHECKMATE":
        this.finishGame("МАТ!");
        break;

      case "STALEMATE":
        this.finishGame("ПАТ!");
        break;
    }
  }

  // ===== board =====

  private onCellClick(coord: string) {
    if (!this.state.isStarted) {
      Toast.show("Сначала начните игру", "error");
      return;
    }

    if (!this.state.selected) {
      this.state.selected = coord;
      this.socket.send("GET_MOVES", { from: coord });
      return;
    }

    this.socket.send("MOVE", {
      from: this.state.selected,
      to: coord
    });

    this.state.resetSelection();
    this.board.clearHighlights();
  }

  // ===== utils =====

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