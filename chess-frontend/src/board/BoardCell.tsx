import React from 'react';
import { PieceType } from './BoardTypes';
import { Piece } from './Piece';

interface BoardCellProps {
  coord: string;
  isLight: boolean;
  isHighlighted: boolean;
  file: string;
  rank: number;
  showFileCoord: boolean;
  showRankCoord: boolean;
  piece?: PieceType;
  onClick: (coord: string) => void;
}

export const BoardCell: React.FC<BoardCellProps> = React.memo(({
  coord,
  isLight,
  isHighlighted,
  file,
  rank,
  showFileCoord,
  showRankCoord,
  piece,
  onClick
}) => (
  <div
    className={`cell ${isLight ? 'light' : 'dark'} ${isHighlighted ? 'highlight' : ''}`}
    data-pos={coord}
    onClick={() => onClick(coord)}
  >
    {showFileCoord && <span className="coord bottom">{file}</span>}
    {showRankCoord && <span className="coord left">{rank}</span>}
    {piece && <Piece piece={piece} />}
  </div>
));