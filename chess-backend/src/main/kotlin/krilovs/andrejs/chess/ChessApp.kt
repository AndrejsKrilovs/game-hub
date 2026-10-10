package krilovs.andrejs.chess

import krilovs.andrejs.chess.application.WebsocketHandler
import org.springframework.beans.factory.annotation.Value
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
class ChessApp(
  private val handler: WebsocketHandler,
  @param:Value($$"${app.websocket.allowed-origins:}")
  private val allowedOrigins: List<String>,
  @param:Value($$"${app.websocket.idle-timeout-seconds:360}")
  private val idleTimeoutSeconds: Long
) : WebSocketConfigurer {

  override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
    val registration = registry.addHandler(handler, "/chess/ws")

    val origins = allowedOrigins
      .map { it.trim() }
      .filter { it.isNotBlank() }

    if (origins.isNotEmpty()) {
      registration.setAllowedOriginPatterns(*origins.toTypedArray())
    }
  }

  @Bean
  fun createWebSocketContainer(): ServletServerContainerFactoryBean {
    val container = ServletServerContainerFactoryBean()
    container.maxSessionIdleTimeout = idleTimeoutSeconds * 1000L
    container.asyncSendTimeout = 5 * 1000L
    return container
  }
}