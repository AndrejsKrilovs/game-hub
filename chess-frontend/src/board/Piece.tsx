import React from 'react';
import { PieceType, pieceMetadata } from './BoardTypes';

interface PieceProps {
  piece: PieceType;
}

export const Piece: React.FC<PieceProps> = ({ piece }) => {
  const symbol = pieceMetadata[piece.type]?.[piece.color];
  if (!symbol) {
    return null;
  }

  return <span data-piece={`${piece.color}_${piece.type}`}>{symbol}</span>;
};