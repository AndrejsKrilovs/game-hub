package krilovs.andrejs.checkers;

import krilovs.andrejs.checkers.application.CheckersWebsocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

@Configuration
@EnableWebSocket
@ConfigurationPropertiesScan
@RequiredArgsConstructor
public class CheckersApp implements WebSocketConfigurer {

  private static final String WS_PATH = "/checkers/ws";

  private final CheckersWebsocketHandler handler;

  @Value("${app.websocket.allowed-origins:}")
  private final List<String> allowedOrigins;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    var registration = registry.addHandler(handler, WS_PATH);

    var origins = allowedOrigins.stream()
      .map(String::trim)
      .filter(origin -> !origin.isBlank())
      .toArray(String[]::new);

    if (origins.length > 0) {
      registration.setAllowedOriginPatterns(origins);
    }
  }
}
