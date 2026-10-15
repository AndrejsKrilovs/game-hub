package krilovs.andrejs.checkers.dto;

import krilovs.andrejs.checkers.domain.model.Move;

public sealed interface MoveResult {
  record Success(Move move) implements MoveResult {
  }

  record Error(String message) implements MoveResult {
  }
}
