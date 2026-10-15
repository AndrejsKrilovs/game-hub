package krilovs.andrejs.checkers.application.bot;

import krilovs.andrejs.checkers.domain.model.Board;
import krilovs.andrejs.checkers.domain.model.Color;
import krilovs.andrejs.checkers.domain.model.Piece;
import krilovs.andrejs.checkers.domain.service.MoveGenerator;
import krilovs.andrejs.checkers.domain.service.Step;
import krilovs.andrejs.checkers.utils.BoardUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Генерирует полные ходы (с серией взятий до конца) с учётом обязательного взятия. */
@Component
@RequiredArgsConstructor
public class TurnGenerator {

  private final MoveGenerator generator;

  public List<BotTurn> generate(Board board, Color color) {
    List<BotTurn> captures = new ArrayList<>();
    for (Map.Entry<Integer, Piece> e : board.pieces().entrySet()) {
      if (e.getValue().color() == color) {
        collectCaptures(board, e.getKey(), captures);
      }
    }
    if (!captures.isEmpty()) {
      return captures;
    }

    List<BotTurn> moves = new ArrayList<>();
    for (Map.Entry<Integer, Piece> e : board.pieces().entrySet()) {
      if (e.getValue().color() != color) {
        continue;
      }
      for (Step step : generator.simpleMoves(board, e.getKey())) {
        moves.add(new BotTurn(e.getKey(), List.of(step.to()), List.of()));
      }
    }
    return moves;
  }

  private void collectCaptures(Board board, int from, List<BotTurn> result) {
    dfs(board, from, from, new HashSet<>(), new ArrayList<>(), new ArrayList<>(), result);
  }

  private void dfs(
    Board board, int from, int current, Set<Integer> taken,
    List<Integer> path, List<Integer> captured, List<BotTurn> result
  ) {
    for (Step step : generator.captures(board, current, taken)) {
      Piece piece = board.get(current);
      boolean promoted = !piece.king() && BoardUtils.rank(step.to()) == BoardUtils.promotionRank(piece.color());

      board.set(current, null);
      board.set(step.to(), promoted ? piece.promoted() : piece);
      taken.add(step.captured());
      path.add(step.to());
      captured.add(step.captured());

      if (generator.captures(board, step.to(), taken).isEmpty()) {
        result.add(new BotTurn(from, List.copyOf(path), List.copyOf(captured)));
      }
      else {
        dfs(board, from, step.to(), taken, path, captured, result);
      }

      captured.removeLast();
      path.removeLast();
      taken.remove(step.captured());
      board.set(step.to(), null);
      board.set(current, piece);
    }
  }
}
