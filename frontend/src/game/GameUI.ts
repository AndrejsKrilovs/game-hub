export class GameUI {
  startBtn!: HTMLButtonElement;
  endBtn!: HTMLButtonElement;
  historyEl!: HTMLTextAreaElement;

  init(onStart: () => void, onEnd: () => void) {
    const sidebar = document.getElementById("sidebar")!;

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

    this.startBtn.onclick = onStart;
    this.endBtn.onclick = onEnd;

    this.endBtn.disabled = true;
  }

  setStarted(started: boolean) {
    this.startBtn.disabled = started;
    this.endBtn.disabled = !started;
  }

  clearHistory() {
    this.historyEl.value = "";
  }

  addToHistory(piece: any, from: string, to: string) {
    const el = this.historyEl;
    const castling = this.isCastling(from, to, piece);
    if (castling) {
      const color = piece.color === "WHITE" ? "Белые" : "Чёрные";
      const type = castling === "short" ? "короткая рокировка" : "длинная рокировка";

      el.value += `${color}: ${type}\n`;
      el.scrollTop = el.scrollHeight;
      return;
    }

    const gender = this.getPieceGender(piece.type);
    const color =
      piece.color === "WHITE"
        ? (gender === "f" ? "Белая" : "Белый")
        : (gender === "f" ? "Чёрная" : "Чёрный");

    const type = this.getPieceName(piece.type).toLowerCase();
    el.value += `${color} ${type}: ${from} → ${to}\n`;
    el.scrollTop = el.scrollHeight;
  }

	private isCastling(from: string, to: string, piece: any): "short" | "long" | null {
    if (piece.type !== "King") return null;

    const fromFile = from.charCodeAt(0);
    const toFile = to.charCodeAt(0);
    const diff = toFile - fromFile;
    if (diff === 2) return "short";
    if (diff === -2) return "long";
    return null;
	}

	private getPieceName(type: string): string {
    return {
      Pawn: "Пешка",
      Rook: "Ладья",
      Knight: "Конь",
      Bishop: "Слон",
      Queen: "Ферзь",
      King: "Король"
    }[type] || type;
  }

  private getPieceGender(type: string): "m" | "f" {
    return {
      Pawn: "f",
      Rook: "f",
      Knight: "m",
      Bishop: "m",
      Queen: "m",
      King: "m"
    }[type] || "m";
  }
}