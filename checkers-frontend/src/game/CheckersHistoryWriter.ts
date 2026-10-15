import { pieceMetadata, PieceColor, PieceName } from '../board/BoardTypes';

export interface CheckersHistoryPayload {
  text?: string;
  color?: PieceColor;
  piece?: PieceName;
  from?: string;
  to?: string;
  captured?: string;
  promoted?: boolean;
  continues?: boolean;
  state?: 'NORMAL' | 'WIN' | string;
}

export const formatCheckersMove = (payload: CheckersHistoryPayload | any): string => {
  if (!payload) {
    return '';
  }
  if (typeof payload.text === 'string' && payload.text.trim()) {
    return payload.text;
  }
  if (!payload.color || !payload.from || !payload.to) {
    return '';
  }

  const pieceColor = payload.color === 'WHITE' ? 'Белые' : 'Чёрные';
  const pieceName = pieceMetadata[payload.piece as keyof typeof pieceMetadata]?.name ?? payload.piece ?? '';
  const separator = payload.captured ? ':' : ' → ';
  const notes = [
    payload.captured ? 'взятие' : '',
    payload.promoted ? 'дамка' : '',
    payload.continues ? 'бьёт дальше' : '',
  ].filter(Boolean);
  const notesText = notes.length ? ` (${notes.join(', ')})` : '';
  return `${pieceColor}: ${pieceName} ${payload.from}${separator}${payload.to}${notesText}`;
};
