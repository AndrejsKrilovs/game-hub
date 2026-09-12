import {useState} from "react";
import {authService} from "./authService.ts";
import {Game} from "./types.ts";
import axios from "axios";
import {eventBus} from "shared-frontend";
import {LoginForm} from "./components/LoginForm.tsx";
import {UserBar} from "./components/UserBar.tsx";
import {GameGrid} from "./components/GameGrid.tsx";
import {GAMES} from "./constants.ts";

export const App = () => {
  const { username, isAuthChecking, login, logout } = authService();
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

  if (isAuthChecking) {
    return <div className="container"><p>Загрузка...</p></div>;
  }

  if (!username) {
    return (
      <div className="container">
        <div className="header">
          <h1>🎮 Game Hub</h1>
          <p>Представьтесь перед началом игры</p>
        </div>
        <LoginForm onLogin={login} />
      </div>
    );
  }

  return (
    <div className="container">
      <div className="header">
        <h1>🎮 Game Hub</h1>
        <UserBar username={username} onLogout={logout} />
      </div>

      <GameGrid
        games={GAMES}
        loadingGameId={loadingGameId}
        onStartGame={handleStartGame}
      />
    </div>
  );
};