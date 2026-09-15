import "./style.css";
import "./mobile.css";
import { createRoot } from "react-dom/client";

import { eventBus } from "./game/EventBus";
import { gameSocket } from "./game/GameSocket";

import { Sidebar, HistoryFormatter } from "./components/Sidebar";
import { Toast } from "./components/Toast";
import { Confetti } from "./components/Confetti";

type GameName = "chess" | "checkers";

const initCap = (value: string): string => {
  const normalized = value.trim();
  if (!normalized) return "";
  return normalized.charAt(0).toUpperCase() + normalized.slice(1).toLowerCase();
};

const ensureBaseLayout = () => {
  if (!document.getElementById("layout")) {
    const layout = document.createElement("div");
    layout.id = "layout";

    const sidebar = document.createElement("div");
    sidebar.id = "sidebar";

    const app = document.createElement("div");
    app.id = "app";

    layout.appendChild(sidebar);
    layout.appendChild(app);
    document.body.appendChild(layout);
  }

  if (!document.getElementById("shared-ui-overlay")) {
    const overlay = document.createElement("div");
    overlay.id = "shared-ui-overlay";
    document.body.appendChild(overlay);
  }
};

const lockBrowserNavigation = (gameName: GameName) => {
  const gamePath = `/${gameName}`;

  history.replaceState({ page: gameName }, "", gamePath);
  history.pushState({ page: `${gameName}-lock` }, "", gamePath);

  window.addEventListener("popstate", () => {
    history.pushState({ page: `${gameName}-lock` }, "", gamePath);
    eventBus.emit("TOAST", { message: "Нет возможности пользоваться навигацией" });
  });
};

const connectWebSocket = (gameName: GameName) => {
  const wsProtocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  const wsUrl = `${wsProtocol}//${window.location.host}/${gameName}/ws`;
  eventBus.emit("WS_CONNECT", wsUrl);
};

export const bootstrapGame = (gameName: GameName, historyFormatter?: HistoryFormatter) => {
  ensureBaseLayout();
  document.title = initCap(gameName);

  const sidebarEl = document.getElementById("sidebar");
  if (!sidebarEl) {
    throw new Error("#sidebar not found");
  }
  const sidebarRoot = createRoot(sidebarEl);
  sidebarRoot.render(
    <Sidebar eventBus={eventBus} historyFormatter={historyFormatter} />
  );

  const overlayEl = document.getElementById("shared-ui-overlay");
  if (!overlayEl) {
    throw new Error("#shared-ui-overlay not found");
  }
  const overlayRoot = createRoot(overlayEl);
  overlayRoot.render(
    <>
      <Toast eventBus={eventBus} />
      <Confetti eventBus={eventBus} />
    </>
  );

  const appContainer = document.querySelector<HTMLElement>("#app");
  if (!appContainer) {
    throw new Error("#app not found");
  }

  gameSocket(eventBus, gameName);
  connectWebSocket(gameName);
  lockBrowserNavigation(gameName);
  return { eventBus, appContainer, sidebarRoot, overlayRoot };
};