package krilovs.andrejs.checkers.domain.model;

public enum Color {
  BLACK,
  WHITE;

  public Color opposite() {
    return this == WHITE ? BLACK : WHITE;
  }
}
