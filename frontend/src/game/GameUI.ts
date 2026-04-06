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

  addHistory(text: string) {
    this.historyEl.value += text + "\n";
    this.historyEl.scrollTop = this.historyEl.scrollHeight;
  }
}