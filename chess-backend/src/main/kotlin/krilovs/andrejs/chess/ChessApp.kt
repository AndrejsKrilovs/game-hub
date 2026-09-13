package krilovs.andrejs.chess

import krilovs.andrejs.chess.application.WebsocketHandler
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean

@Component
@EnableWebSocket
@ConfigurationPropertiesScan
class ChessApp(private val handler: WebsocketHandler) : WebSocketConfigurer {
  override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
    registry
      .addHandler(handler, "/chess/ws")
      .setAllowedOriginPatterns("*")
  }

  @Bean
  fun createWebSocketContainer(): ServletServerContainerFactoryBean {
    val container = ServletServerContainerFactoryBean()
    container.maxSessionIdleTimeout = 15 * 60 * 1000L
    container.asyncSendTimeout = 5 * 1000L
    return container
  }
}