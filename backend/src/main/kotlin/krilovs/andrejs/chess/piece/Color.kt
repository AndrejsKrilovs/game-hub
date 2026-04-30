package krilovs.andrejs.chess.piece

enum class Color {
  BLACK,
  WHITE;

  fun opposite(): Color = if (this == WHITE) BLACK else WHITE
}