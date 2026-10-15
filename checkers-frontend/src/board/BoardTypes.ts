export type MoveHighlight = { to: string }
export type BoardPerspective = PieceColor;
export type PieceColor = "WHITE" | "BLACK";
export type PieceName = "Man" | "King";

export type PieceType = {
  type: PieceName;
  color: PieceColor;
  coordinates: string;
  captured?: boolean;
};

export const pieceMetadata = {
  Man: { name: "шашка" },
  King: { name: "дамка" },
} as const;
