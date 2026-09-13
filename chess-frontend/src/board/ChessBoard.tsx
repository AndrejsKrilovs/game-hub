import React, { useMemo } from 'react';
import { EventBus } from 'shared-frontend';
import { PieceType } from './BoardTypes';
import { BoardCell } from './BoardCell';
import { useBoardState } from './useBoardState';
import { useGameController } from '../game/useGameController';

interface ChessBoardProps {
  eventBus: EventBus;
}

const FILES = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
const RANKS = [8, 7, 6, 5, 4, 3, 2, 1];

export const ChessBoard: React.FC<ChessBoardProps> = ({ eventBus }) => {
  useGameController(eventBus);

  const { perspective, pieces, highlights, isFinished, isStarted } = useBoardState(eventBus);

  const activeFiles = useMemo(
    () => (perspective === 'WHITE' ? FILES : [...FILES].reverse()),
    [perspective]
  );

  const activeRanks = useMemo(
    () => (perspective === 'WHITE' ? RANKS : [...RANKS].reverse()),
    [perspective]
  );

  const pieceMap = useMemo(() => {
    const map = new Map<string, PieceType>();
    pieces.forEach((p) => map.set(p.coordinates, p));
    return map;
  }, [pieces]);

  const handleCellClick = (coord: string) => {
    eventBus.emit('CELL_CLICK', { cord: coord });
  };

  if (!isStarted) {
    return null;
  }

  return (
    <div className={`board ${isFinished ? 'finished' : ''}`}>
      {activeRanks.map((rank, rIndex) =>
        activeFiles.map((file, fIndex) => {
          const coord = `${file}${rank}`;
          return (
            <BoardCell
              key={coord}
              coord={coord}
              file={file}
              rank={rank}
              isLight={(rIndex + fIndex) % 2 === 0}
              isHighlighted={highlights.has(coord)}
              showFileCoord={rIndex === 7}
              showRankCoord={fIndex === 0}
              piece={pieceMap.get(coord)}
              onClick={handleCellClick}
            />
          );
        })
      )}
    </div>
  );
};