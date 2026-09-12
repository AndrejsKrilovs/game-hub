import { Game } from "../types";

interface GameCardProps {
  game: Game;
  isLoading: boolean;
  onStart: (game: Game) => void;
}

export const GameCard = ({ game, isLoading, onStart }: GameCardProps) => {
  const { active, icon, title } = game;

  return (
    <div className={`game-card ${!active ? "disabled" : ""}`}>
      <div className="game-icon">{icon}</div>
      <h2 className="game-title">{title}</h2>

      <span className={`badge ${active ? "badge-active" : "badge-soon"}`}>
        {active ? "Доступно" : "Скоро"}
      </span>

      <button
        type="button"
        className={`btn ${active ? "btn-start" : ""}`}
        disabled={!active || isLoading}
        onClick={() => onStart(game)}
      >
        {isLoading ? "Загрузка..." : active ? "Играть" : "В разработке"}
      </button>
    </div>
  );
};