package krilovs.andrejs.chess

import krilovs.andrejs.chess.application.WebsocketHandler
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.stereotype.Component
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Component
@EnableWebSocket
@ConfigurationPropertiesScan
class ChessApp(private val handler: WebsocketHandler) : WebSocketConfigurer {
  override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
    registry
      .addHandler(handler, "/chess/ws")
      .setAllowedOriginPatterns("*")
  }
}