import "./style.css"

import { eventBus } from "./game/EventBus"
import { gameSocket } from "./game/GameSocket"
import { confettiController } from "./game/ConfettiController"

import { sidebarComponent } from "./sidebar/SidebarComponent"
import { sidebarController } from "./sidebar/SidebarController"
import type { HistoryFormatter } from "./sidebar/SidebarController"

import { toastController } from "./toast/ToastController"
import {UserConfig} from "vite";

type GameName = "chess" | "checkers"
type Component = { init?: (el: HTMLElement) => void }
type Controller = { control: (bus: typeof eventBus, el: HTMLElement) => void }

const initCap = (value: string): string => {
  const normalized = value.trim()

  if (!normalized) {
    return ""
  }

  return normalized.charAt(0).toUpperCase() + normalized.slice(1).toLowerCase()
}

const ensureBaseLayout = () => {
  if (!document.getElementById("layout")) {
    const layout = document.createElement("div")
    layout.id = "layout"

    const sidebar = document.createElement("div")
    sidebar.id = "sidebar"

    const app = document.createElement("div")
    app.id = "app"

    layout.appendChild(sidebar)
    layout.appendChild(app)
    document.body.appendChild(layout)
  }

  if (!document.querySelector(".toast")) {
    const toast = document.createElement("div")
    toast.className = "toast"
    document.body.appendChild(toast)
  }
}

const setup = (selector: string, component: Component, controller: Controller) => {
  const el = document.querySelector<HTMLElement>(selector)

  if (!el) {
    throw new Error(`${selector} not found`)
  }

  component.init?.(el)
  controller.control(eventBus, el)
  return el
}

const lockBrowserNavigation = (gameName: GameName) => {
  const gamePath = `/${gameName}`

  history.replaceState({ page: gameName }, "", gamePath)
  history.pushState({ page: `${gameName}-lock` }, "", gamePath)

  window.addEventListener("popstate", () => {
    history.pushState({ page: `${gameName}-lock` }, "", gamePath)
    eventBus.emit("TOAST", { message: "Нет возможности пользоваться навигацией" })
  })
}

const connectWebSocket = (gameName: GameName) => {
  const wsProtocol = window.location.protocol === "https:" ? "wss:" : "ws:"
  const wsUrl = `${wsProtocol}//${window.location.host}/${gameName}/ws`
  eventBus.emit("WS_CONNECT", wsUrl)
}

export const bootstrapGame = (gameName: GameName, historyFormatter?: HistoryFormatter) => {
  ensureBaseLayout()
  document.title = initCap(gameName)

  if (historyFormatter) {
    sidebarController.setHistoryFormatter(historyFormatter)
  }

  setup("#sidebar", sidebarComponent, sidebarController)

  const toastContainer = setup(".toast", { init: () => {} }, toastController)
  const appContainer = document.querySelector<HTMLElement>("#app")

  if (!appContainer) {
    throw new Error("#app not found")
  }

  confettiController.control(eventBus)
  gameSocket(eventBus, gameName)
  connectWebSocket(gameName)
  lockBrowserNavigation(gameName)
  return { eventBus, appContainer, toastContainer }
}

export interface GameConfigOptions {
  base: string;
  title: string;
}

export const createGameConfig = ({ base, title }: GameConfigOptions): UserConfig => {
  const htmlContent = `<!DOCTYPE html>
<html lang="ru">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>${title}</title>
    </head>
    <body>
        <script type="module" src="/src/main.ts"></script>
    </body>
</html>`;

  return {
    base,
    plugins: [
      {
        name: 'virtual-html-plugin',
        configureServer(server: any) {
          server.middlewares.use((req: any, res: any, next: any) => {
            if (req.url === '/' || req.url === '/index.html') {
              res.setHeader('Content-Type', 'text/html');
              return res.end(htmlContent);
            }
            next();
          });
        },
        resolveId(id: string) {
          if (id === 'index.html' || id === '/index.html') {
            return 'index.html';
          }
        },
        load(id: string) {
          if (id === 'index.html') {
            return htmlContent;
          }
        },
      },
    ],
    build: {
      emptyOutDir: true,
      rolldownOptions: {
        input: 'index.html',
      },
    },
  };
};