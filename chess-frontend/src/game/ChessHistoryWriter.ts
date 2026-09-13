import { pieceMetadata, PieceColor, PieceName } from '../board/BoardTypes';

export interface ChessHistoryPayload {
  text?: string;
  color?: PieceColor;
  castlingType?: 'SHORT' | 'LONG';
  piece?: PieceName;
  from?: string;
  to?: string;
  state?: 'CHECK' | 'CHECKMATE' | string;
}

const STATE_TEXT_MAP: Record<string, string> = {
  CHECK: ' (шах)',
  CHECKMATE: ' (мат)',
};

export const formatChessMove = (payload: ChessHistoryPayload | any): string => {
  if (!payload) {
    return '';
  }
  if (typeof payload.text === 'string' && payload.text.trim()) {
    return payload.text;
  }
  if (!payload.color || (!payload.castlingType && (!payload.from || !payload.to))) {
    return '';
  }

  const pieceColor = payload.color === 'WHITE' ? 'Белые' : 'Чёрные';
  if (payload.castlingType) {
    const castlingText =
      payload.castlingType === 'SHORT' ? 'короткая рокировка' : 'длинная рокировка';
    return `${pieceColor}: ${castlingText}`;
  }

  const pieceName =
    pieceMetadata[payload.piece as keyof typeof pieceMetadata]?.name ?? payload.piece ?? '';

  const stateText = STATE_TEXT_MAP[payload.state ?? ''] ?? '';
  return `${pieceColor}: ${pieceName} ${payload.from} → ${payload.to}${stateText}`;
};