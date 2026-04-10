import { Toast } from "./Toast"

export class GameUI {
  constructor(private bus: EventBus) {}

  actionBtn!: HTMLButtonElement;
  historyEl!: HTMLTextAreaElement;

  private onStart!: () => void;
  private onEnd!: () => void;
  private started = false;

  init(onStart: () => void, onEnd: () => void) {
    this.onStart = onStart;
    this.onEnd = onEnd;

    const sidebar = document.getElementById("sidebar")!;

    sidebar.innerHTML = `
      <button class="btn btn-start" data-action>Начать игру</button>
      <div class="history">
        <label>История ходов</label>
        <textarea readonly></textarea>
      </div>
    `;

    this.actionBtn = sidebar.querySelector("[data-action]")!;
    this.historyEl = sidebar.querySelector("textarea")!;
    this.actionBtn.onclick = this.handleAction;
    this.bus.on("TOAST", ({ message, type }) => { new Toast(this.bus) });
  }

  private handleAction = () => {
    if (this.started) {
      this.onEnd();
    }
    else {
      this.onStart();
    }
  };

  setStarted(started: boolean) {
    this.started = started;
    this.actionBtn.textContent = started ? "Завершить игру" : "Начать игру";
    this.actionBtn.classList.toggle("btn-start", !started);
    this.actionBtn.classList.toggle("btn-end", started);
  }

  clearHistory() {
    this.historyEl.value = "";
  }

  addToHistory(piece: any, from: string, to: string, extra?: any) {
    const color = piece.color === "WHITE" ? "Белые" : "Чёрные";

    if (extra?.isCastling) {
      const type =
        extra.castlingType === "SHORT"
          ? "короткая рокировка"
          : "длинная рокировка";

      this.historyEl.value += `${color}: ${type}\n`;
      this.historyEl.scrollTop = this.historyEl.scrollHeight;
      return;
    }

    const pieceNames: Record<string, string> = {
      Pawn: "пешка",
      Rook: "ладья",
      Knight: "конь",
      Bishop: "слон",
      Queen: "ферзь",
      King: "король"
    };

    const pieceName = pieceNames[piece.type] ?? piece.type;
    this.historyEl.value += `${color}: ${pieceName} ${from} → ${to}\n`;
    this.historyEl.scrollTop = this.historyEl.scrollHeight;
  }
}