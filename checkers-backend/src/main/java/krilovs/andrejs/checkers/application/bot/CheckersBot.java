package krilovs.andrejs.checkers.application.bot;

import krilovs.andrejs.checkers.application.CheckersGameService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckersBot {

  private final CheckersSearch search;
  private final BotProperties properties;

  /** Лучший ход для стороны, чья очередь ходить. Игра не изменяется. */
  public BotTurn findBestTurn(CheckersGameService game) {
    return search.findBestTurn(
      game.snapshotBoard(),
      game.getCurrentTurn(),
      properties.maxDepth(),
      properties.timeLimitMs()
    );
  }
}
