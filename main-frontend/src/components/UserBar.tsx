interface UserBarProps {
  username: string;
  onLogout: () => void;
}

export const UserBar = ({ username, onLogout }: UserBarProps) => (
  <div className="user-bar">
    <span className="user-greeting">
      Добро пожаловать, <strong>{username}</strong>
    </span>
    <button type="button" onClick={onLogout} className="btn btn-logout">
      Выйти
    </button>
  </div>
);