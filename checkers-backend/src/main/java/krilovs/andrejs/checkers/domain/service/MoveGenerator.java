package krilovs.andrejs.checkers.domain.service;

import krilovs.andrejs.checkers.domain.model.Board;
import krilovs.andrejs.checkers.domain.model.Color;
import krilovs.andrejs.checkers.domain.model.Piece;
import krilovs.andrejs.checkers.utils.BoardUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Генерация ходов русских шашек. Побитые в текущей серии фигуры (taken) остаются на доске
 * до конца хода: через них нельзя перепрыгнуть повторно и они блокируют путь.
 */
@Component
public class MoveGenerator {

  private static final int[][] DIRECTIONS = {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
  private static final int[] PAWN_FILES = {1, -1};

  private record Jump(int captured, List<Integer> landings) {
  }

  public List<Step> captures(Board board, int square, Set<Integer> taken) {
    Piece piece = board.get(square);
    if (piece == null) {
      return List.of();
    }

    return piece.king() ? kingCaptures(board, square, piece, taken) : manCaptures(board, square, piece, taken);
  }

  public List<Step> simpleMoves(Board board, int square) {
    Piece piece = board.get(square);
    if (piece == null) {
      return List.of();
    }

    List<Step> result = new ArrayList<>();
    int file = BoardUtils.file(square);
    int rank = BoardUtils.rank(square);

    if (piece.king()) {
      for (int[] d : DIRECTIONS) {
        int f = file + d[0];
        int r = rank + d[1];
        while (BoardUtils.inside(f, r) && board.get(BoardUtils.square(f, r)) == null) {
          result.add(Step.move(BoardUtils.square(f, r)));
          f += d[0];
          r += d[1];
        }
      }
      return result;
    }

    int r = rank + BoardUtils.forward(piece.color());
    for (int df : PAWN_FILES) {
      int f = file + df;
      if (BoardUtils.inside(f, r) && board.get(BoardUtils.square(f, r)) == null) {
        result.add(Step.move(BoardUtils.square(f, r)));
      }
    }
    return result;
  }

  public boolean hasAnyCapture(Board board, Color color, Set<Integer> taken) {
    for (Map.Entry<Integer, Piece> e : board.pieces().entrySet()) {
      if (e.getValue().color() == color && !captures(board, e.getKey(), taken).isEmpty()) {
        return true;
      }
    }
    return false;
  }

  public boolean hasAnyMove(Board board, Color color) {
    Set<Integer> none = Set.of();
    if (hasAnyCapture(board, color, none)) {
      return true;
    }

    for (Map.Entry<Integer, Piece> e : board.pieces().entrySet()) {
      if (e.getValue().color() == color && !simpleMoves(board, e.getKey()).isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private List<Step> manCaptures(Board board, int square, Piece piece, Set<Integer> taken) {
    List<Step> result = new ArrayList<>();
    int file = BoardUtils.file(square);
    int rank = BoardUtils.rank(square);

    for (int[] d : DIRECTIONS) {
      int landFile = file + 2 * d[0];
      int landRank = rank + 2 * d[1];
      if (!BoardUtils.inside(landFile, landRank)) {
        continue;
      }

      int mid = BoardUtils.square(file + d[0], rank + d[1]);
      int land = BoardUtils.square(landFile, landRank);
      Piece target = board.get(mid);

      if (target != null && target.color() != piece.color() && !taken.contains(mid) && board.get(land) == null) {
        result.add(new Step(land, mid));
      }
    }
    return result;
  }

  private List<Step> kingCaptures(Board board, int square, Piece piece, Set<Integer> taken) {
    List<Step> result = new ArrayList<>();

    for (Jump jump : kingJumps(board, square, piece, taken)) {
      Set<Integer> next = new HashSet<>(taken);
      next.add(jump.captured());

      // Если после взятия можно продолжить бить, дамка обязана встать на такую клетку
      List<Integer> continuing = jump.landings().stream()
        .filter(land -> canContinue(board, square, land, piece, next))
        .toList();

      for (int land : continuing.isEmpty() ? jump.landings() : continuing) {
        result.add(new Step(land, jump.captured()));
      }
    }
    return result;
  }

  private List<Jump> kingJumps(Board board, int square, Piece piece, Set<Integer> taken) {
    List<Jump> result = new ArrayList<>();

    for (int[] d : DIRECTIONS) {
      int f = BoardUtils.file(square) + d[0];
      int r = BoardUtils.rank(square) + d[1];

      while (BoardUtils.inside(f, r) && board.get(BoardUtils.square(f, r)) == null) {
        f += d[0];
        r += d[1];
      }
      if (!BoardUtils.inside(f, r)) {
        continue;
      }

      int mid = BoardUtils.square(f, r);
      Piece target = board.get(mid);
      if (target.color() == piece.color() || taken.contains(mid)) {
        continue;
      }

      List<Integer> landings = new ArrayList<>();
      int lf = f + d[0];
      int lr = r + d[1];
      while (BoardUtils.inside(lf, lr) && board.get(BoardUtils.square(lf, lr)) == null) {
        landings.add(BoardUtils.square(lf, lr));
        lf += d[0];
        lr += d[1];
      }

      if (!landings.isEmpty()) {
        result.add(new Jump(mid, landings));
      }
    }
    return result;
  }

  private boolean canContinue(Board board, int from, int land, Piece piece, Set<Integer> taken) {
    board.set(from, null);
    board.set(land, piece);
    try {
      return !kingJumps(board, land, piece, taken).isEmpty();
    }
    finally {
      board.set(land, null);
      board.set(from, piece);
    }
  }
}
