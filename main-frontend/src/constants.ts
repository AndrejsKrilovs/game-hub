import { Game } from "./types";

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