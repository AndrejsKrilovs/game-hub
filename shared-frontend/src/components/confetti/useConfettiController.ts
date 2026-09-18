import { useEffect, useState } from 'react';
import { map, tap, timer } from 'rxjs';

import type { EventBus } from '../../game/EventBus';
import { APP_EVENTS } from '../../game/events';

export interface ConfettiPiece {
  id: number;
  left: string;
  animationDuration: string;
  animationDelay: string;
  transform: string;
}

const PIECE_COUNT = 90;
const CONFETTI_DURATION_MS = 4200;

const createPieces = (): ConfettiPiece[] => {
  return Array.from({ length: PIECE_COUNT }, (_, index) => ({
    id: index,
    left: `${Math.random() * 100}%`,
    animationDuration: `${1.8 + Math.random() * 1.8}s`,
    animationDelay: `${Math.random() * 0.4}s`,
    transform: `rotate(${Math.random() * 360}deg)`,
  }));
};

export const useConfettiController = (eventBus: EventBus): ConfettiPiece[] | null => {
  const [pieces, setPieces] = useState<ConfettiPiece[] | null>(null);

  useEffect(() => {
    const subscription = eventBus
      .observe(APP_EVENTS.confetti)
      .pipe(
        map(() => createPieces()),
        tap(setPieces)
      )
      .subscribe();

    return () => subscription.unsubscribe()
  }, [eventBus]);

  useEffect(() => {
    if (!pieces) {
      return;
    }

    const subscription = timer(CONFETTI_DURATION_MS).subscribe(() => {
      setPieces(null);
    });

    return () => subscription.unsubscribe()
  }, [pieces]);

  return pieces;
};