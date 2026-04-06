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
      const { piece, from, to } = this.state.pendingMove;
      this.ui.addToHistory(piece, from, to);
      this.state.pendingMove = null;
    }

    this.state.resetSelection();
    this.board.clearHighlights();
    this.board.render(data.pieces, this.getSymbol);

    switch (data.state) {
      case "CHECK":
        Toast.show("ШАХ!");
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
        Toast.show(`Фигура на ${this.state.selected} не имеет ходов`,"info");
        this.state.resetSelection();
        this.board.clearHighlights();
        return;
      }

      this.state.availableMoves = data.moves;
      this.board.highlight(data.moves);
	}

	private handleInvalidMove(data: any) {
    this.state.pendingMove = null;
    const moves = data.availableMoves ?? [];
    const text = moves.length
      ? `Некорректный ход. Доступные: ${moves.join(", ")}`
      : "Некорректный ход";
    Toast.show(text, "error");

    if (moves.length) {
      this.state.availableMoves = moves;
      this.board.highlight(moves);
    }
  }

	private handleError(data: any) {
    this.state.resetSelection();
    this.board.clearHighlights();
    Toast.show(data.message ?? "Ошибка", "error");
	}

  // ===== board =====

  private onCellClick(coord: string) {
    if (!this.state.isStarted) {
      Toast.show("Сначала начните игру", "error");
      return;
    }

    if (this.state.gameOver) {
      Toast.show("Игра завершена", "error");
      return;
    }
    if (!this.state.selected) {
      const piece = this.getPiece(coord);

      if (!piece) {
        Toast.show("Клетка пустая", "error");
        return;
      }
      if (piece.color !== this.state.currentTurn) {
        const turn = this.state.currentTurn === "WHITE" ? "белых" : "чёрных";
        Toast.show(`Сейчас ход ${turn}`, "error");
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
      Toast.show("Недопустимый ход. Повторите попытку", "error");
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

	private getPiece(coord: string) {
    return this.state.pieces.find(
      p => `${p.coordinates.file}${p.coordinates.rank}` === coord
    );
  }
}