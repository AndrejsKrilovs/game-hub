package krilovs.andrejs.chess.game

import krilovs.andrejs.chess.dto.AvailableMovesResult
import krilovs.andrejs.chess.dto.MoveResult
import krilovs.andrejs.chess.piece.Bishop
import krilovs.andrejs.chess.piece.Color
import krilovs.andrejs.chess.piece.King
import krilovs.andrejs.chess.piece.Knight
import krilovs.andrejs.chess.piece.Pawn
import krilovs.andrejs.chess.piece.Piece
import krilovs.andrejs.chess.piece.Queen
import krilovs.andrejs.chess.piece.Rook
import org.springframework.stereotype.Component

@Component
class Board {
  private val board = Array<Piece?>(64) { null }
  private var availableMoves = emptySet<Move>()
  operator fun get(square: Int) = board[square]
  operator fun set(square: Int, piece: Piece?) { board[square] = piece }

  var currentColor = Color.WHITE
  val pieces: List<Piece> get() = board.filterNotNull()

  fun loadFromFEN(fen: String) {
    board.fill(null)

    val (boardPart, turnPart, castlingPart) = fen.split(" ").let {
      Triple(it[0], it[1], it.getOrElse(2) { "-" })
    }

    var file = 0
    var rank = 7

    boardPart.forEach { char ->
      when {
        char == '/' -> {
          rank--
          file = 0
        }
        char.isDigit() -> file += char.digitToInt()
        else -> {
          val color = if (char.isUpperCase()) Color.WHITE else Color.BLACK
          val square = rank * 8 + file
          this[square] = createPiece(char.lowercaseChar(), color, square)
          file++
        }
      }
    }
  }

  fun generateMovesForSquare(square: Int): AvailableMovesResult {
    val piece = this[square] ?: return AvailableMovesResult.Error("Пустая клетка")

    if (piece.color != currentColor) {
      return AvailableMovesResult.Error("Фигура другого цвета")
    }

    availableMoves = piece.generateMoves().filter { it.piece.color != this[it.to.toSquare()]?.color }.toSet()
    return when {
      availableMoves.isEmpty() -> AvailableMovesResult.Error("Нет доступных ходов")
      else -> AvailableMovesResult.Success(availableMoves)
    }
  }

  fun makeMove(from: Int, to: Int): MoveResult {
    val move = availableMoves.firstOrNull { it.to.toSquare() == to } ?: return MoveResult.Error("Некорректный ход")

    this[from] = null
    val piece = move.piece
    piece.square = to

    this[to] = piece
    availableMoves = emptySet()
    currentColor = currentColor.opposite()
    return MoveResult.Success(move)
  }

  private fun createPiece(type: Char?, color: Color, square: Int): Piece =
    when (type?.lowercaseChar()) {
      'p' -> Pawn(color, square)
      'r' -> Rook(color, square)
      'n' -> Knight(color, square)
      'b' -> Bishop(color, square)
      'q' -> Queen(color, square)
      'k' -> King(color, square)
      else -> error("Unknown piece: $type")
    }

  private fun String.toSquare(): Int = (this[1].digitToInt() - 1) * 8 + (this[0] - 'a')
}