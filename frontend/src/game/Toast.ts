import { getSymbol } from "./Util";

type Color = "WHITE" | "BLACK";

export class Toast {
  private element: HTMLDivElement;

  constructor(private bus: EventBus) {
    const existing = document.querySelector<HTMLDivElement>(".toast");
    if (existing) {
      this.element = existing;
    }
		else {
      this.element = document.createElement("div");
      this.element.className = "toast";
      document.body.appendChild(this.element);
    }

    this.bind();
  }

  private bind = (): void => {
    this.bus.on("TOAST", ({ message, type }) => {
      this.showMessage(message, type);
    });
    this.bus.on("OPEN_COLOR_PICKER", (onStart: (c: Color) => void) => {
      this.showColorPicker(onStart);
    });
    this.bus.on("CONFIRM", ({ message, onConfirm }) => {
      this.showConfirm(message, onConfirm);
    });
		this.bus.on("OPEN_PROMOTION_DIALOG", ({ pieces, color, onSelect }) => {
      this.showPiecePicker(pieces, color, onSelect);
    });
  };

  private show = (html: string, className = ""): void => {
    this.element.innerHTML = "";
    this.element.className = `toast show ${className}`;
    this.element.innerHTML = html;
  };

  private hide = (): void => {
    this.element.className = "toast";
    this.element.innerHTML = "";
  };

  private showMessage = (message: string, type: "info" | "error" | "success" = "info"): void => {
    const icons = { info: "ℹ️", error: "❌", success: "✅" };
    this.show(
      `
      <div class="toast-content">
        <div class="toast-message">
          <span>${icons[type]}</span>
          ${message}
        </div>
      </div>
    `,
      type
    );

    setTimeout(this.hide, 2000);
  };

  private showColorPicker = (onStart: (color: Color) => void): void => {
    let selected: Color | null = null;
    this.show(`
      <div class="toast-content">
        <div>Выберите цвет фигур</div>

        <div class="toast-actions">
          <button class="btn btn-end" data-c="WHITE">Белые</button>
          <button class="btn btn-end" data-c="BLACK">Чёрные</button>
        </div>

        <button class="btn btn-start" data-start>Старт</button>
      </div>
    `);

    const buttons = [...this.element.querySelectorAll<HTMLButtonElement>("[data-c]")];
    const start = this.element.querySelector<HTMLButtonElement>("[data-start]");

    buttons.forEach(btn => {
      btn.onclick = () => {
        const c = btn.dataset.c as Color;
        selected = c;
        buttons.forEach(b => b.classList.remove("btn-selected"));
        btn.classList.add("btn-selected");
      };
    });

    start!.onclick = () => {
      if (!selected) return;
      this.hide();
      onStart(selected);
    };
  };

  private showConfirm = (message: string, onConfirm: () => void): void => {
    this.show(`
      <div class="toast-content">
        <div>${message}</div>
        <div class="toast-actions">
          <button class="btn btn-end" data-ok>Да</button>
          <button class="btn btn-start" data-cancel>Отмена</button>
        </div>
      </div>
    `);

    this.element.querySelector("[data-ok]")!.onclick = () => {
      this.hide();
      onConfirm();
    };

    this.element.querySelector("[data-cancel]")!.onclick = this.hide;
  };

	private showPiecePicker = (
    pieces: any[],
    color: string,
    onSelect: (piece: any) => void
  ): void => {

    this.show(`
      <div class="toast-content">
        <div>Выберите фигуру</div>
        <div class="toast-actions">
          ${pieces.map(p => `
            <button class="btn btn-piece" data-piece="${p}">${getSymbol(p, color)}</button>
          `).join("")}
        </div>
      </div>
    `);

    this.element
      .querySelectorAll<HTMLButtonElement>("[data-piece]")
      .forEach(btn => {
        btn.onclick = () => {
          this.hide();
          onSelect(btn.dataset.piece!);
        };
      });
  };
}