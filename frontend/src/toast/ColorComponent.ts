class ColorComponent {
  init = (root: HTMLElement) => {
    root.innerHTML = `
			<div class="toast-content">
        <div>Выберите цвет фигур</div>

        <div class="toast-actions">
          <button class="btn btn-end" data-color="WHITE">Белые</button>
          <button class="btn btn-end" data-color="BLACK">Чёрные</button>
        </div>

        <button class="btn btn-start" data-start>Старт</button>
      </div>
    `
  }
}

export const colorComponent = new ColorComponent()