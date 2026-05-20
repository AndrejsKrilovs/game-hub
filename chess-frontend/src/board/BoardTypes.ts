export type BoardPerspective = PieceColor
export type PieceColor = "WHITE" | "BLACK"
export type PieceName = "Pawn" | "Rook" | "Knight" | "Bishop" | "Queen" | "King"

export type PieceType = {
  type: PieceName
  color: PieceColor
  coordinates: String
}