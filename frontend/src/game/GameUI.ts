import { EventBus } from "../EventBus";
import { Toast } from "./Toast";

export class GameUI {
  constructor(private bus: EventBus) {}

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

    this.bus.on("TOAST", ({ message, type }) => {
      Toast.show(message, type);
    });

    this.bus.on("MOVE_DONE", ({ piece, from, to }) => {
      this.addToHistory(piece, from, to);
    });
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

    const color = piece.color === "WHITE" ? "Белые" : "Чёрные";
    el.value += `${color}: ${from} → ${to}\n`;

    el.scrollTop = el.scrollHeight;
  }
}