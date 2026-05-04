package krilovs.andrejs.chess

import krilovs.andrejs.chess.application.WebsocketHandler
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@EnableWebSocket
@SpringBootApplication
class Application(private val handler: WebsocketHandler) : WebSocketConfigurer {
  override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
    registry.addHandler(handler, "/ws")
  }
}

fun main(args: Array<String>) {
  runApplication<Application>(*args)
}