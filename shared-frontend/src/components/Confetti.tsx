import React, { useState, useEffect } from 'react';
import { EventBus } from '../game/EventBus';
import { useEvent } from '../game/useEvent';

interface Piece {
  id: number;
  left: string;
  animationDuration: string;
  animationDelay: string;
  transform: string;
}

interface ConfettiProps {
  eventBus: EventBus;
}

const PIECE_COUNT = 90;

const createPieces = (): Piece[] =>
  Array.from({ length: PIECE_COUNT }, (_, i) => ({
    id: i,
    left: `${Math.random() * 100}%`,
    animationDuration: `${1.8 + Math.random() * 1.8}s`,
    animationDelay: `${Math.random() * 0.4}s`,
    transform: `rotate(${Math.random() * 360}deg)`,
  }));

export const Confetti: React.FC<ConfettiProps> = ({ eventBus }) => {
  const [pieces, setPieces] = useState<Piece[] | null>(null);
  useEvent(eventBus, 'CONFETTI', () => setPieces(createPieces()));

  useEffect(() => {
    if (!pieces) return;
    const timer = setTimeout(() => setPieces(null), 4200);
    return () => clearTimeout(timer);
  }, [pieces]);

  if (!pieces) {
    return null
  }
  return (
    <div className="confetti-layer">
      {pieces.map((piece) => (
        <span
          key={piece.id}
          className="confetti-piece"
          style={{
            left: piece.left,
            animationDuration: piece.animationDuration,
            animationDelay: piece.animationDelay,
            transform: piece.transform,
          }}
        />
      ))}
    </div>
  );
};