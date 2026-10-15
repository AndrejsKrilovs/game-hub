package krilovs.andrejs.checkers.application.bot;

import java.util.List;

/** Полный ход: путь фигуры (для серии взятий несколько клеток) и клетки побитых фигур. */
public record BotTurn(int from, List<Integer> path, List<Integer> captured) {

  public int to() {
    return path.getLast();
  }

  public boolean isCapture() {
    return !captured.isEmpty();
  }
}
