import React from 'react';
import { PieceType } from './BoardTypes';

interface PieceProps {
  piece: PieceType;
}

export const Piece: React.FC<PieceProps> = ({ piece }) => (
  <span
    data-piece={`${piece.color}_${piece.type}`}
    className={`checker ${piece.color.toLowerCase()} ${piece.captured ? 'captured' : ''}`}
  >
    {piece.type === 'King' ? '♛' : ''}
  </span>
);
