package krilovs.andrejs.checkers.application.bot;

import krilovs.andrejs.checkers.domain.model.Board;
import krilovs.andrejs.checkers.domain.model.Color;
import krilovs.andrejs.checkers.domain.model.Piece;
import krilovs.andrejs.checkers.utils.BoardUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Negamax с альфа-бета отсечением, итеративным углублением и ограничением по времени. */
@Component
@RequiredArgsConstructor
public class CheckersSearch {

  private static final int INF = 1_000_000;
  private static final int MAX_PLY = 40;
  private static final int TIME_CHECK_MASK = 255;
  private static final Comparator<BotTurn> CAPTURES_FIRST =
    Comparator.comparingInt((BotTurn t) -> t.captured().size()).reversed();

  private final TurnGenerator turns;
  private final BoardEvaluator evaluator;

  /** Ищет лучший ход на копии доски. Возвращает null, если ходов нет. */
  public BotTurn findBestTurn(Board board, Color color, int maxDepth, long timeLimitMs) {
    return new Searcher(board.copy(), color, System.currentTimeMillis() + timeLimitMs).run(maxDepth);
  }

  private record Undo(BotTurn turn, Piece piece, Map<Integer, Piece> removed) {
  }

  private final class Searcher {
    private final Board board;
    private final Color root;
    private final long deadline;
    private long nodes;
    private boolean aborted;

    Searcher(Board board, Color root, long deadline) {
      this.board = board;
      this.root = root;
      this.deadline = deadline;
    }

    BotTurn run(int maxDepth) {
      var rootTurns = new ArrayList<>(turns.generate(board, root));
      if (rootTurns.isEmpty()) {
        return null;
      }

      Collections.shuffle(rootTurns); // равноценные ходы выбираются случайно
      rootTurns.sort(CAPTURES_FIRST);
      BotTurn best = rootTurns.getFirst();
      if (rootTurns.size() == 1) {
        return best;
      }

      for (int depth = 1; depth <= maxDepth; depth++) {
        BotTurn depthBest = null;
        int alpha = -INF;

        for (BotTurn turn : rootTurns) {
          Undo undo = apply(turn);
          int score = -negamax(root.opposite(), depth - 1, 1, -INF, -alpha);
          revert(undo);

          if (aborted) {
            return best;
          }
          if (depthBest == null || score > alpha) {
            depthBest = turn;
            alpha = Math.max(alpha, score);
          }
        }

        best = depthBest;
        rootTurns.remove(best);
        rootTurns.addFirst(best); // лучший ход предыдущей глубины проверяем первым
      }
      return best;
    }

    private int negamax(Color side, int depth, int ply, int alpha, int beta) {
      if ((++nodes & TIME_CHECK_MASK) == 0 && System.currentTimeMillis() >= deadline) {
        aborted = true;
      }
      if (aborted) {
        return 0;
      }

      List<BotTurn> moves = turns.generate(board, side);
      if (moves.isEmpty()) {
        return -(INF - ply);
      }

      // Пока есть обязательное взятие, позицию не оцениваем (аналог quiescence)
      boolean forced = moves.getFirst().isCapture();
      if (depth <= 0 && (!forced || ply >= MAX_PLY)) {
        return evaluator.evaluate(board, side);
      }

      moves.sort(CAPTURES_FIRST);
      int best = -INF;

      for (BotTurn turn : moves) {
        Undo undo = apply(turn);
        int score = -negamax(side.opposite(), depth - 1, ply + 1, -beta, -alpha);
        revert(undo);

        if (aborted) {
          return 0;
        }
        best = Math.max(best, score);
        alpha = Math.max(alpha, score);
        if (alpha >= beta) {
          break;
        }
      }
      return best;
    }

    private Undo apply(BotTurn turn) {
      Piece piece = board.get(turn.from());
      Map<Integer, Piece> removed = new LinkedHashMap<>();
      for (int square : turn.captured()) {
        removed.put(square, board.get(square));
        board.set(square, null);
      }

      int promotionRank = BoardUtils.promotionRank(piece.color());
      boolean promote = !piece.king() && turn.path().stream().anyMatch(s -> BoardUtils.rank(s) == promotionRank);

      board.set(turn.from(), null);
      board.set(turn.to(), promote ? piece.promoted() : piece);
      return new Undo(turn, piece, removed);
    }

    private void revert(Undo undo) {
      board.set(undo.turn().to(), null);
      board.set(undo.turn().from(), undo.piece());
      undo.removed().forEach(board::set);
    }
  }
}
