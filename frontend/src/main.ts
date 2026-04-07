import './style.css'
import { Game } from './game/Game'
import { EventBus } from './EventBus'

const bus = new EventBus()

new Game(bus)