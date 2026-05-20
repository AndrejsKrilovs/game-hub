import { pieceMetadata } from "../board/PieceComponent"

class PromotionComponent {
  init = (root: HTMLElement, data?: any) => {
    root.innerHTML = `
			<div class="toast-content">
        <div>Выберите фигуру</div>
        <div class="toast-actions">
          ${
      		  data.availablePieces
      			  .map(p => `<button class="btn btn-end" data-piece="${p}">${pieceMetadata[p]?.[data.color] ?? "?"}</button>`)
      				.join("")
          }
        </div>
      </div>
    `
  }
}

export const promotionComponent = new PromotionComponent()