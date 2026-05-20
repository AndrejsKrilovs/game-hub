package krilovs.andrejs.chess.application.bot

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "chess.bot")
data class BotProperties(
  val maxDepth: Int = 4,
  val timeLimitMs: Long = 1200
)