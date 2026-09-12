import { GAMES } from "./constants";
import { useAuthService } from "./hooks/useAuthService";
import { useGameLauncher } from "./hooks/useGameLauncher";
import { LoginForm } from "./components/LoginForm";
import { UserBar } from "./components/UserBar";
import { GameGrid } from "./components/GameGrid";

export const App = () => {
  const { username, isAuthChecking, login, logout } = useAuthService();
  const { loadingGameId, startGame } = useGameLauncher();

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
        onStartGame={startGame}
      />
    </div>
  );
};