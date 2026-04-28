export type PieceName = "Pawn" | "Rook" | "Knight" | "Bishop" | "Queen" | "King"

export type Coordinates = {
  file: "a" | "b" | "c" | "d" | "e" | "f" | "g" | "h"
  rank: 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8
}

export type PieceType = {
  type: PieceName
  color: string
  coordinates: Coordinates
}