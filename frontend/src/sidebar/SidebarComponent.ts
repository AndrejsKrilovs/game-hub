class SidebarComponent {
  init = (eventBus: EventBus, root: HTMLElement) => {
    root.innerHTML = `
      <button class="btn btn-start">Начать игру</button>
      <button class="btn btn-end">Завершить игру</button>
      <div class="history">
        <label>История ходов</label>
        <textarea readonly></textarea>
      </div>
    `

    const startBtn: HTMLButtonElement = root.querySelector(".btn-start")!
    startBtn.onclick = () => eventBus.emit("START_GAME")

		const endBtn: HTMLButtonElement = root.querySelector(".btn-end")!
    endBtn.classList.add("hidden")
    endBtn.onclick = () => eventBus.emit("END_GAME")
  }
}

export const sidebarComponent = new SidebarComponent()