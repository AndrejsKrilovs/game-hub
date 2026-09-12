import { useState } from "react";
import axios from "axios";
import { eventBus } from "shared-frontend";

export interface Game {
  id: string;
  title: string;
  icon: string;
  active: boolean;
  startEndpoint?: string;
  targetUrl?: string;
}

export const GAMES: Game[] = [
  {
    id: "chess",
    title: "Шахматы",
    icon: "♔",
    active: true,
    startEndpoint: "/games/chess/start",
    targetUrl: "/chess/"
  },
  { id: "blackjack", title: "Blackjack", icon: "🂠", active: false },
  { id: "checkers", title: "Шашки", icon: "⛀", active: false },
  { id: "tic-tac-toe", title: "Крестики-Нолики", icon: "✕◯", active: false }
];

export const App = () => {
  const [loadingGameId, setLoadingGameId] = useState<string | null>(null);

  const handleStartGame = async (game: Game) => {
    if (!game.active || !game.startEndpoint || !game.targetUrl) return;

    setLoadingGameId(game.id);

    try {
      await axios.post(game.startEndpoint);
      window.location.replace(game.targetUrl);
    }
    catch (err) {
      let message = "Не удалось запустить игру. Попробуйте снова.";
      if (axios.isAxiosError(err) && err.response?.data?.message) {
        message = err.response.data.message;
      }

      eventBus.emit("TOAST", { message, type: "error" });
      setLoadingGameId(null);
    }
  };

  return (
    <div className="container">
      <div className="header">
        <h1>🎮 Game Hub</h1>
        <p>Выберите игру для начала партии</p>
      </div>

      <div className="games-grid">
        {GAMES.map((game) => {
          const isLoading = loadingGameId === game.id;

          return (
            <div
              key={game.id}
              className={`game-card ${!game.active ? "disabled" : ""}`}
            >
              <div className="game-icon">{game.icon}</div>
              <h2 className="game-title">{game.title}</h2>

              <span className={`badge ${game.active ? "badge-active" : "badge-soon"}`}>
                {game.active ? "Доступно" : "Скоро"}
              </span>

              <button
                type="button"
                className={`btn ${game.active ? "btn-start" : ""}`}
                disabled={!game.active || isLoading}
                onClick={() => handleStartGame(game)}
              >
                {isLoading ? "Загрузка..." : game.active ? "Играть" : "В разработке"}
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
};