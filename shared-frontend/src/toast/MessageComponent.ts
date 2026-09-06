class MessageComponent {
  init = (root: HTMLElement, message?: string) => {
    root.innerHTML = `
			<div class="toast-content">
        <div class="toast-message">
          <span>ℹ️</span>
          <div class="toast-text">${message}</div>
        </div>
      </div>
    `
  }
}

export const messageComponent = new MessageComponent()