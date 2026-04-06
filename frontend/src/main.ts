import './style.css'
import { Game } from './game/Game'
import { EventBus } from './EventBus'

new Game(new EventBus());