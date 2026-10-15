package krilovs.andrejs.checkers.utils;

import krilovs.andrejs.checkers.domain.model.Color;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BoardUtils {

  public final int SIZE = 8;

  public String toCord(int square) {
    return "" + (char) ('a' + file(square)) + (rank(square) + 1);
  }

  public int toSquare(String cell) {
    if (cell == null || cell.length() != 2) {
      throw new IllegalArgumentException("Некорректная клетка: " + cell);
    }

    int file = cell.charAt(0) - 'a';
    int rank = cell.charAt(1) - '1';
    if (!inside(file, rank)) {
      throw new IllegalArgumentException("Некорректная клетка: " + cell);
    }

    return square(file, rank);
  }

  public int file(int square) {
    return square % SIZE;
  }

  public int rank(int square) {
    return square / SIZE;
  }

  public int square(int file, int rank) {
    return rank * SIZE + file;
  }

  public boolean inside(int file, int rank) {
    return file >= 0 && file < SIZE && rank >= 0 && rank < SIZE;
  }

  public boolean isDark(int file, int rank) {
    return (file + rank) % 2 == 0;
  }

  public int forward(Color color) {
    return color == Color.WHITE ? 1 : -1;
  }

  public int promotionRank(Color color) {
    return color == Color.WHITE ? SIZE - 1 : 0;
  }
}
