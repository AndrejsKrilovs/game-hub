package krilovs.andrejs.chess.board

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
class GameBoard {
  private val board = Array<Piece?>(64) { null }
  operator fun get(square: Int) = board[square]
  operator fun set(square: Int, piece: Piece?) { board[square] = piece }

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
}