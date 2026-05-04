package krilovs.andrejs.chess.domain.model

enum class Color {
  BLACK,
  WHITE;

  fun opposite(): Color = if (this == WHITE) BLACK else WHITE
}