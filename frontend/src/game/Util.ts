const map: Record<string, Record<string, string>> = {
	Pawn: { WHITE: "♙", BLACK: "♟" },
  Rook: { WHITE: "♖", BLACK: "♜" },
  Knight: { WHITE: "♘", BLACK: "♞" },
  Bishop: { WHITE: "♗", BLACK: "♝" },
  Queen: { WHITE: "♕", BLACK: "♛" },
  King: { WHITE: "♔", BLACK: "♚" }
};

export const getSymbol = (type: string, color: string): string => map[type]?.[color] ?? "?";