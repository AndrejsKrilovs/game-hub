import { bootstrapGame } from "shared-frontend"

import { chessHistoryWriter } from "./game/ChessHistoryWriter"
import { gameController } from "./game/GameController"

import { promotionController } from "./toast/PromotionController"

import { boardController } from "./board/BoardController"

const { eventBus, appContainer, toastContainer} = bootstrapGame("chess", chessHistoryWriter.writeHistory)

boardController.control(eventBus, appContainer)
promotionController.control(eventBus, toastContainer)
gameController.control(eventBus)