package krilovs.andrejs.checkers.application.bot;

import krilovs.andrejs.checkers.domain.model.Board;
import krilovs.andrejs.checkers.domain.model.Color;
import krilovs.andrejs.checkers.domain.model.Piece;
import krilovs.andrejs.checkers.utils.BoardUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BoardEvaluator {

  private static final int MAN = 100;
  private static final int KING = 320;
  private static final int ADVANCE = 6;
  private static final int CENTER = 3;
  private static final int BACK_RANK = 4;

  /** Оценка позиции с точки зрения {@code side}: положительная — в пользу side. */
  public int evaluate(Board board, Color side) {
    int score = 0;

    for (Map.Entry<Integer, Piece> e : board.pieces().entrySet()) {
      int value = value(e.getKey(), e.getValue());
      score += e.getValue().color() == side ? value : -value;
    }
    return score;
  }

  private int value(int square, Piece piece) {
    int file = BoardUtils.file(square);
    int rank = BoardUtils.rank(square);
    boolean central = file >= 2 && file <= 5 && rank >= 2 && rank <= 5;

    if (piece.king()) {
      return KING + (central ? CENTER : 0);
    }

    int progress = piece.color() == Color.WHITE ? rank : BoardUtils.SIZE - 1 - rank;
    int value = MAN + progress * ADVANCE + (central ? CENTER : 0);
    if (progress == 0) {
      value += BACK_RANK;
    }
    return value;
  }
}
