export class Toast {
  static show(message: string, type: "info" | "error" | "success" = "info") {
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
}