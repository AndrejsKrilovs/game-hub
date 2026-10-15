package krilovs.andrejs.checkers.domain.model;

import krilovs.andrejs.checkers.utils.BoardUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class Board {

  private final Piece[] squares = new Piece[BoardUtils.SIZE * BoardUtils.SIZE];

  public Piece get(int square) {
    return squares[square];
  }

  public void set(int square, Piece piece) {
    squares[square] = piece;
  }

  public Board copy() {
    Board copy = new Board();
    System.arraycopy(squares, 0, copy.squares, 0, squares.length);
    return copy;
  }

  public void clear() {
    Arrays.fill(squares, null);
  }

  /** Снимок: клетка -> фигура. Безопасен для изменения доски во время обхода. */
  public Map<Integer, Piece> pieces() {
    Map<Integer, Piece> result = new LinkedHashMap<>();
    for (int square = 0; square < squares.length; square++) {
      if (squares[square] != null) {
        result.put(square, squares[square]);
      }
    }
    return result;
  }
}
