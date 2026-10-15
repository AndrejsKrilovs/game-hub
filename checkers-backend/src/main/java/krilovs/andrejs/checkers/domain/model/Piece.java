package krilovs.andrejs.checkers.domain.model;

public record Piece(Color color, boolean king) {

  public String type() {
    return king ? "King" : "Man";
  }

  public Piece promoted() {
    return new Piece(color, true);
  }
}
