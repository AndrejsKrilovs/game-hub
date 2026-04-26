import "./style.css"
import { eventBus } from "./game/EventBus"
import { gameSocket } from "./game/GameSocket"
import { gameController } from "./game/GameController"

import { sidebarComponent } from "./sidebar/SidebarComponent"
import { sidebarController } from "./sidebar/SidebarController"

import { toastController } from "./toast/ToastController"
import { boardController } from "./board/BoardController"

const setup = (
  selector: string,
  component: { init?: (el: HTMLElement) => void },
  controller: { control: (bus: typeof eventBus, el: HTMLElement) => void }
) => {
  const el = document.querySelector<HTMLElement>(selector)
  if (!el) throw new Error(`${selector} not found`)
  component.init?.(el)
  controller.control(eventBus, el)
}

setup("#sidebar", sidebarComponent, sidebarController)
setup(".toast", { init: () => {} }, toastController)
setup("#app", { init: () => {} }, boardController)

gameSocket(eventBus)
gameController.control(eventBus)
eventBus.emit("WS_CONNECT", "ws://localhost:8080/ws")