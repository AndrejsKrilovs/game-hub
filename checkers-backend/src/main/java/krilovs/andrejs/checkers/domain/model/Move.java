package krilovs.andrejs.checkers.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;

public record Move(
  String from,
  String to,
  Piece piece,
  String captured,
  boolean promoted,
  boolean continues
) {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record MoveDto(
    String from,
    String to,
    String piece,
    Color color,
    String captured,
    boolean promoted,
    boolean continues
  ) {
  }

  public MoveDto toDto() {
    return new MoveDto(from, to, piece.type(), piece.color(), captured, promoted, continues);
  }
}
