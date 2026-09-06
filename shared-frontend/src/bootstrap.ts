import "./style.css"

import { eventBus } from "./game/EventBus"
import { gameSocket } from "./game/GameSocket"
import { confettiController } from "./game/ConfettiController"

import { sidebarComponent } from "./sidebar/SidebarComponent"
import { sidebarController } from "./sidebar/SidebarController"
import type { HistoryFormatter } from "./sidebar/SidebarController"

import { toastController } from "./toast/ToastController"

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