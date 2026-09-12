import {ChangeEvent, useState} from "react";

interface LoginFormProps {
  onLogin: (name: string) => Promise<void>;
}

export const LoginForm = ({ onLogin }: LoginFormProps) => {
  const [inputName, setInputName] = useState("");

  const handleSubmit = (e: ChangeEvent<HTMLFormElement>) => {
    e.preventDefault();
    void onLogin(inputName);
  };

  return (
    <form onSubmit={handleSubmit} className="game-card auth-card">
      <div className="game-icon">👋</div>
      <h2 className="game-title">Вход</h2>
      <input
        type="text"
        className="auth-input"
        placeholder="Ваше игровое имя"
        value={inputName}
        onChange={(e) => setInputName(e.target.value)}
        autoFocus
      />
      <button type="submit" className="btn btn-start" disabled={!inputName.trim()}>
        Войти
      </button>
    </form>
  );
};