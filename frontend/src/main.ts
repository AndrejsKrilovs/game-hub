import "./style.css"
import { eventBus } from "./EventBus"

import { sidebarComponent } from "./sidebar/SidebarComponent"
import { sidebarController } from "./sidebar/SidebarController"

const setup = (
  selector: string,
  component: { init?: (el: HTMLElement) => void },
  controller: { control: (bus: typeof eventBus, el: HTMLElement) => void }
) => {
  const el = document.querySelector<HTMLElement>(selector)
  if (!el) throw new Error(`${selector} not found`)

  component.init?.(el)
  controller.control(eventBus, el)
  return el
}

setup("#sidebar", sidebarComponent, sidebarController)