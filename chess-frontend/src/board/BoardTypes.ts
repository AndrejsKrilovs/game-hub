export type MoveHighlight = { to: string }
export type BoardPerspective = PieceColor;
export type PieceColor = "WHITE" | "BLACK";
export type PieceName = "Pawn" | "Rook" | "Knight" | "Bishop" | "Queen" | "King";

export type PieceType = {
  type: PieceName;
  color: PieceColor;
  coordinates: string;
};

export const pieceMetadata = {
  Pawn: { name: "пешка", WHITE: "♙", BLACK: "♟" },
  Rook: { name: "ладья", WHITE: "♖", BLACK: "♜" },
  Knight: { name: "конь", WHITE: "♘", BLACK: "♞" },
  Bishop: { name: "слон", WHITE: "♗", BLACK: "♝" },
  Queen: { name: "ферзь", WHITE: "♕", BLACK: "♛" },
  King: { name: "король", WHITE: "♔", BLACK: "♚" },
} as const;