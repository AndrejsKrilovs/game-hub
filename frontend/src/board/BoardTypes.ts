export type PieceName = "Pawn" | "Rook" | "Knight" | "Bishop" | "Queen" | "King"

export type PieceType = {
  type: PieceName
  color: string
  coordinates: String
}