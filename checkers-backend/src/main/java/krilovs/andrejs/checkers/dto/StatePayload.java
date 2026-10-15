package krilovs.andrejs.checkers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import krilovs.andrejs.checkers.domain.model.Color;
import krilovs.andrejs.checkers.domain.model.GameState;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StatePayload(List<PieceDto> pieces, Color turn, GameState state, Color winner) {
}
