package krilovs.andrejs.checkers.domain.service;

/** Один шаг фигуры: клетка назначения и (для взятия) клетка побитой фигуры. */
public record Step(int to, Integer captured) {

  public static Step move(int to) {
    return new Step(to, null);
  }
}
