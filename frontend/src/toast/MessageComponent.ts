class MessageComponent {
  init = (root: HTMLElement, message?: string) => {
    root.innerHTML = `
			<div class="toast-content">
        <div class="toast-message">ℹ️ ${message}</div>
      </div>
    `
  }
}

export const messageComponent = new MessageComponent()