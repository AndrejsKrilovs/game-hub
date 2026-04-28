class SidebarComponent {
  init = (root: HTMLElement) => {
    root.innerHTML = `
      <button class="btn btn-start" data-start>Начать игру</button>
      <button class="btn btn-end hidden" data-end>Завершить игру</button>
      <div class="history">
        <label>История ходов</label>
        <textarea readonly></textarea>
      </div>
    `
  }
}

export const sidebarComponent = new SidebarComponent()