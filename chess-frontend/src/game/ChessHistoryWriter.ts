import { pieceMetadata } from "../board/PieceComponent"

class ChessHistoryWriter {
  writeHistory = (payload: any): string => {
    if (payload.text) {
      return String(payload.text)
    }

    const pieceColor = payload.color === "WHITE" ? "Белые" : "Чёрные"

    if (payload.castlingType) {
      return `${pieceColor}: ${
        payload.castlingType === "SHORT"
          ? "короткая рокировка"
          : "длинная рокировка"
      }`
    }

    const pieceName =
      pieceMetadata[payload.piece as keyof typeof pieceMetadata]?.name
      ?? payload.piece

    const stateTextByState: Record<string, string> = {
      CHECK: " (шах)",
      CHECKMATE: " (мат)"
    }

    const stateText = stateTextByState[payload.state ?? ""] ?? ""
    return `${pieceColor}: ${pieceName} ${payload.from} → ${payload.to}${stateText}`
  }
}

export const chessHistoryWriter = new ChessHistoryWriter()