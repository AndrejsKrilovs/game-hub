import React, { useState } from 'react';
import { EventBus, useEvent } from 'shared-frontend';
import { pieceMetadata, PieceName, PieceColor } from '../board/BoardTypes';

export interface PromotionMove {
  from: string;
  to: string;
  piece?: string;
  color: PieceColor;
}

export interface PromotionPayload {
  move: PromotionMove;
  availablePieces: PieceName[];
}

interface PromotionModalProps {
  eventBus: EventBus;
}

export const PromotionModal: React.FC<PromotionModalProps> = ({ eventBus }) => {
  const [data, setData] = useState<PromotionPayload | null>(null);

  useEvent(eventBus, 'WS:PROMOTION', (payload: PromotionPayload) => {
    if (payload?.move && payload?.availablePieces) {
      setData(payload);
    }
  });

  if (!data) {
    return null;
  }

  const handleSelectPiece = (piece: PieceName) => {
    eventBus.emit('WS_SEND', {
      type: 'PROMOTE',
      payload: {...data.move, piece }
    });
    setData(null);
  };

  return (
    <div className="toast info show">
      <div className="toast-content">
        <div>Выберите фигуру</div>
        <div className="toast-actions">
          {data.availablePieces.map((piece) => (
            <button
              key={piece}
              className="btn btn-end"
              onClick={() => handleSelectPiece(piece)}
            >
              {pieceMetadata[piece]?.[data.move.color] ?? '?'}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};