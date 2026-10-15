package krilovs.andrejs.checkers.dto;

import krilovs.andrejs.checkers.domain.model.Color;

/** Фигура для клиента. {@code captured} — побита в текущей серии, но ход ещё не завершён. */
public record PieceDto(String type, Color color, String coordinates, boolean captured) {
}
