import React from 'react';
import type { EventBus } from '../../game/EventBus';
import { useConfettiController } from './useConfettiController';

interface ConfettiProps {
  eventBus: EventBus;
}

export const Confetti: React.FC<ConfettiProps> = ({ eventBus }) => {
  const pieces = useConfettiController(eventBus);
  if (!pieces) {
    return null;
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