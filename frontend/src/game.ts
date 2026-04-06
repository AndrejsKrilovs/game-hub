import { BoardView } from "./board";

type Piece = {
    type: string;
    color: string;
    coordinates: { file: string; rank: number };
};

export class Game {
    private ws!: WebSocket;
    private board: BoardView | null = null;
    private pieces: Piece[] = [];
    private selected: string | null = null;
    private currentTurn: string = "WHITE";
    private availableMoves: string[] = [];
    private gameOver = false;
    private isGameStarted = false;
    private historyEl!: HTMLTextAreaElement;
    private startBtn!: HTMLButtonElement;
    private endBtn!: HTMLButtonElement;
    private pendingMove: { piece: Piece; from: string; to: string } | null = null;

    constructor() {
        this.initUI();
    }

    private initUI() {
        const app = document.getElementById("app")!;
        const sidebar = document.getElementById("sidebar")!;
        app.innerHTML = ""; // пусто до старта

        sidebar.innerHTML = `
          <button class="btn btn-start">Начать игру</button>
          <button class="btn btn-end">Завершить игру</button>

          <div class="history">
            <label>История ходов</label>
            <textarea readonly></textarea>
          </div>
        `;

        this.startBtn = sidebar.querySelector(".btn-start")!;
        this.endBtn = sidebar.querySelector(".btn-end")!;
        this.historyEl = sidebar.querySelector("textarea")!;
        this.startBtn.addEventListener("click", () => this.startGame());
        this.endBtn.addEventListener("click", () => this.endGame());
        this.endBtn.disabled = true;
    }

    private createBoard() {
        const app = document.getElementById("app")!;
        app.innerHTML = "";
        this.board = new BoardView(this.onCellClick.bind(this));
        this.board.render(app);
    }

    private destroyBoard() {
        const app = document.getElementById("app")!;
        app.innerHTML = "";
        this.board = null;
    }

    private startGame() {
        if (this.isGameStarted) return;
        this.isGameStarted = true;
        this.gameOver = false;
        this.historyEl.value = "";
        this.resetSelection();
        this.startBtn.disabled = true;
        this.endBtn.disabled = false;
        this.createBoard();
        this.initSocket();
        this.showToast("Игра началась", "success");
    }

    private endGame() {
        if (!this.isGameStarted) return;
        this.finishGame("Игра завершена. Начать новую?");
    }

    private finishGame(message: string) {
        this.gameOver = true;
        this.isGameStarted = false;
        this.startBtn.disabled = false;
        this.endBtn.disabled = true;
        this.ws?.close();
        this.destroyBoard();
        this.showToast(message, "info");
    }

    private initSocket() {
        this.ws = new WebSocket("ws://localhost:8080/ws");
        this.ws.onmessage = ({ data }) => {
            const msg = JSON.parse(data);
            this.handleMessage(msg);
        };
    }

    private send(type: string, payload: any) {
        this.ws.send(JSON.stringify({ type, ...payload }));
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
                this.pendingMove = null;
                this.resetSelection();
                this.showError(`Некорректный ход`);
                break;
            case "ERROR":
                this.resetSelection();
                this.showError(data.message);
                break;
        }
    }

    private handleState(data: any) {
        this.currentTurn = data.turn;
        this.render(data.pieces);

        if (this.pendingMove) {
            const { piece, from, to } = this.pendingMove;
            this.addToHistory(piece, from, to);
            this.pendingMove = null;
        }
        this.resetSelection();

        switch (data.state) {
            case "CHECK":
                this.showInfo("ШАХ!");
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
        if (!this.selected || !this.board) return;
        this.availableMoves = data.moves;
        this.board.highlightMoves(this.availableMoves);
    }

    private render(pieces: Piece[]) {
        if (!this.board) return;
        this.pieces = pieces;
        this.board.clear();
        this.board.clearHighlights();
        pieces.forEach(p => {
            const pos = `${p.coordinates.file}${p.coordinates.rank}`;
            this.board!.setPiece(pos, this.getSymbol(p.type, p.color));
        });
    }

    private onCellClick(coord: string) {
        if (!this.isGameStarted) {
            this.showError("Сначала начните игру");
            return;
        }
        if (this.gameOver) {
            this.showError("Игра завершена");
            return;
        }
        if (!this.selected) {
            this.selectCell(coord);
        }
				else {
            this.makeMove(coord);
        }
    }

    private selectCell(coord: string) {
        const piece = this.getPiece(coord);

        if (!piece) return this.showError("Пустая клетка");
        if (piece.color !== this.currentTurn)
            return this.showError(`Ходят ${this.getTurnLabel(this.currentTurn)}`);

        this.selected = coord;
        this.board?.clearHighlights();
        this.send("GET_MOVES", { from: coord });
    }

    private makeMove(to: string) {
        if (!this.selected) return;
        const piece = this.getPiece(this.selected);
        if (!piece) return;

        this.pendingMove = { piece, from: this.selected, to };
        this.send("MOVE", { from: this.selected, to });
    }

    private resetSelection() {
        this.selected = null;
        this.availableMoves = [];
        this.board?.clearHighlights();
    }

    private getPiece(coord: string): Piece | undefined {
        return this.pieces.find(
            p => `${p.coordinates.file}${p.coordinates.rank}` === coord
        );
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

    private getTurnLabel(color: string): string {
        return { WHITE: "белые", BLACK: "чёрные" }[color] || color;
    }

    private showError(msg: string) {
        this.showToast(msg, "error");
    }

    private showInfo(msg: string) {
        this.showToast(msg, "info");
    }

    private showToast(message: string, type: "info" | "error" | "success" = "info") {
        let toast = document.querySelector(".toast") as HTMLElement;

        if (!toast) {
            toast = document.createElement("div");
            toast.className = "toast";
            document.body.appendChild(toast);
        }

        const icons = {
            info: "ℹ️",
            error: "❌",
            success: "✅"
        };

        toast.className = `toast show ${type}`;
        toast.innerHTML = `
          <span class="toast-icon">${icons[type]}</span>
          <span>${message}</span>
        `;

        clearTimeout((toast as any)._timer);
        (toast as any)._timer = setTimeout(() => {
            toast.classList.remove("show");
        }, 2500);
    }

    private addToHistory(piece: Piece, from: string, to: string) {
        const color = piece.color === "WHITE" ? "Белые" : "Чёрные";
        const type = piece.type;

        this.historyEl.value += `${color} ${type}: ${from} → ${to}\n`;
        this.historyEl.scrollTop = this.historyEl.scrollHeight;
    }
}