import { pieceMetadata } from "../board/PieceComponent"

type PieceType = keyof typeof pieceMetadata

export interface PromotionData {
  availablePieces: PieceType[]
  color: "WHITE" | "BLACK"
}

class PromotionComponent {
  init = (root: HTMLElement, data?: PromotionData) => {
    if (!data) return

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