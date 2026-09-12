import { Game } from "../types";
import { GameCard } from "./GameCard";

interface GameGridProps {
  games: Game[];
  loadingGameId: string | null;
  onStartGame: (game: Game) => void;
}

export const GameGrid = ({ games, loadingGameId, onStartGame }: GameGridProps) => (
  <div className="games-grid">
    {games.map((game) => (
      <GameCard
        key={game.id}
        game={game}
        isLoading={loadingGameId === game.id}
        onStart={onStartGame}
      />
    ))}
  </div>
);