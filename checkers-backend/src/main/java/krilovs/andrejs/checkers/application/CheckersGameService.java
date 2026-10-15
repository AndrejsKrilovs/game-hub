package krilovs.andrejs.checkers.application;

import krilovs.andrejs.checkers.domain.model.Board;
import krilovs.andrejs.checkers.domain.model.Color;
import krilovs.andrejs.checkers.domain.model.GameState;
import krilovs.andrejs.checkers.domain.model.Move;
import krilovs.andrejs.checkers.domain.model.Piece;
import krilovs.andrejs.checkers.domain.service.MoveGenerator;
import krilovs.andrejs.checkers.domain.service.Step;
import krilovs.andrejs.checkers.dto.AvailableMovesResult;
import krilovs.andrejs.checkers.dto.MoveResult;
import krilovs.andrejs.checkers.utils.BoardUtils;
import krilovs.andrejs.checkers.dto.PieceDto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CheckersGameService {

  private static final int START_RANKS = 3;
  private static final int NO_CHAIN = -1;

  private final MoveGenerator generator;
  private final Board board = new Board();
  private final Set<Integer> taken = new HashSet<>();

  /** Клетка шашки, обязанной продолжить серию взятий; NO_CHAIN — серии нет. */
  private int chainSquare = NO_CHAIN;
  @Getter
  private Color currentTurn = Color.WHITE;
  @Getter
  private Color winner;

  public GameState getGameState() {
    return winner == null ? GameState.NORMAL : GameState.WIN;
  }

  /** Копия доски для бота. Корректна только в начале хода (вне серии взятий). */
  public Board snapshotBoard() {
    return board.copy();
  }

  public Piece getPiece(int square) {
    return board.get(square);
  }

  public void reset() {
    board.clear();
    taken.clear();
    chainSquare = NO_CHAIN;
    currentTurn = Color.WHITE;
    winner = null;

    for (int rank = 0; rank < BoardUtils.SIZE; rank++) {
      for (int file = 0; file < BoardUtils.SIZE; file++) {
        if (!BoardUtils.isDark(file, rank)) {
          continue;
        }

        if (rank < START_RANKS) {
          board.set(BoardUtils.square(file, rank), new Piece(Color.WHITE, false));
        }
        else if (rank >= BoardUtils.SIZE - START_RANKS) {
          board.set(BoardUtils.square(file, rank), new Piece(Color.BLACK, false));
        }
      }
    }
  }

  /** Фигуры для клиента. Побитые в текущей серии помечены captured, пока ход не завершён. */
  public List<PieceDto> getPiecesDto() {
    return board.pieces().entrySet().stream()
      .map(e -> new PieceDto(
        e.getValue().type(),
        e.getValue().color(),
        BoardUtils.toCord(e.getKey()),
        taken.contains(e.getKey())
      ))
      .toList();
  }

  public AvailableMovesResult generateMovesForSquare(int square) {
    if (winner != null) {
      return new AvailableMovesResult.Error("Игра окончена");
    }

    Piece piece = board.get(square);
    if (piece == null) {
      return new AvailableMovesResult.Error("Пустая клетка");
    }
    if (piece.color() != currentTurn) {
      return new AvailableMovesResult.Error("Фигура другого цвета");
    }
    if (chainSquare != NO_CHAIN && chainSquare != square) {
      return new AvailableMovesResult.Error("Нужно продолжить взятие той же фигурой");
    }

    List<Step> steps = generator.captures(board, square, taken);
    if (steps.isEmpty()) {
      if (generator.hasAnyCapture(board, currentTurn, taken)) {
        return new AvailableMovesResult.Error("Обязательно нужно бить");
      }
      steps = generator.simpleMoves(board, square);
    }
    if (steps.isEmpty()) {
      return new AvailableMovesResult.Error("Нет доступных ходов");
    }

    Set<Move> moves = new LinkedHashSet<>();
    for (Step step : steps) {
      String captured = step.captured() == null ? null : BoardUtils.toCord(step.captured());
      moves.add(new Move(BoardUtils.toCord(square), BoardUtils.toCord(step.to()), piece, captured, false, false));
    }
    return new AvailableMovesResult.Success(moves);
  }

  public MoveResult makeMove(int from, int to) {
    AvailableMovesResult available = generateMovesForSquare(from);
    if (available instanceof AvailableMovesResult.Error(String message)) {
      return new MoveResult.Error(message);
    }

    Set<Move> moves = ((AvailableMovesResult.Success) available).moves();
    String target = BoardUtils.toCord(to);
    Move move = moves.stream().filter(m -> m.to().equals(target)).findFirst().orElse(null);
    if (move == null) {
      return new MoveResult.Error("Некорректный ход");
    }

    Piece piece = board.get(from);
    boolean promoted = !piece.king() && BoardUtils.rank(to) == BoardUtils.promotionRank(piece.color());

    board.set(from, null);
    board.set(to, promoted ? piece.promoted() : piece);

    if (move.captured() != null) {
      taken.add(BoardUtils.toSquare(move.captured()));

      // Серия взятий: ход остаётся у той же фигуры (шашка, дошедшая до края, продолжает бить как дамка)
      if (!generator.captures(board, to, taken).isEmpty()) {
        chainSquare = to;
        return new MoveResult.Success(new Move(move.from(), move.to(), piece, move.captured(), promoted, true));
      }
    }

    finishTurn();
    return new MoveResult.Success(new Move(move.from(), move.to(), piece, move.captured(), promoted, false));
  }

  private void finishTurn() {
    taken.forEach(square -> board.set(square, null));
    taken.clear();
    chainSquare = NO_CHAIN;
    currentTurn = currentTurn.opposite();

    if (!generator.hasAnyMove(board, currentTurn)) {
      winner = currentTurn.opposite();
    }
  }
}
