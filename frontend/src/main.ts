import './style.css'
import { eventBus } from './EventBus'
import { sidebarComponent } from './sidebar/SidebarComponent'
import { sidebarController } from './sidebar/SidebarController'

const rootElement: HTMLElement = document.getElementById("sidebar")!
sidebarComponent.init(eventBus, rootElement)
sidebarController.control(eventBus, rootElement)