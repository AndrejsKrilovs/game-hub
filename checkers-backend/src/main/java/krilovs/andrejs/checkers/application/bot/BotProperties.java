package krilovs.andrejs.checkers.application.bot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "checkers.bot")
public record BotProperties(
  @DefaultValue("8") int maxDepth,
  @DefaultValue("1200") long timeLimitMs
) {
}
