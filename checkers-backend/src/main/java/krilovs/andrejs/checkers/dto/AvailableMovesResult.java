package krilovs.andrejs.checkers.dto;

import krilovs.andrejs.checkers.domain.model.Move;

import java.util.Set;

public sealed interface AvailableMovesResult {
  record Success(Set<Move> moves) implements AvailableMovesResult {
  }

  record Error(String message) implements AvailableMovesResult {
  }
}
