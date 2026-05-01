package krilovs.andrejs.chess.game

import krilovs.andrejs.chess.dto.AvailableMovesResult
import krilovs.andrejs.chess.dto.MoveResult
import krilovs.andrejs.chess.dto.PromotionResult
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
class BoardService(private val ruleService: RuleService) {
  private val board = Array<Piece?>(64) { null }
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

    currentColor = if (turnPart == "w") Color.WHITE else Color.BLACK
  }

  fun generateMovesForSquare(square: Int): AvailableMovesResult {
    val piece = this[square] ?: return AvailableMovesResult.Error("Пустая клетка")

    if (piece.color != currentColor) {
      return AvailableMovesResult.Error("Фигура другого цвета")
    }

    val availableMoves = piece.generateAvailableMoves(this)
      .filter { to -> ruleService.isSafeMove(this, square, to) }
      .map { Move(BoardUtils.toCord(square), BoardUtils.toCord(it), piece) }
      .toSet()

    return when {
      availableMoves.isEmpty() -> AvailableMovesResult.Error("Нет доступных ходов")
      else -> AvailableMovesResult.Success(availableMoves)
    }
  }

  fun makeMove(from: Int, to: Int): MoveResult {
    val piece = this[from] ?: return MoveResult.Error("Пустая клетка")

    if (to !in piece.generateAvailableMoves(this) || !ruleService.isSafeMove(this, from, to)) {
      return MoveResult.Error("Некорректный ход")
    }
    if (ruleService.isPromotion(piece, to)) {
      val move = Move(BoardUtils.toCord(from), BoardUtils.toCord(to), piece)
      return MoveResult.Promotion(
        availablePieces = setOf("Queen", "Rook", "Bishop", "Knight"),
        move = move
      )
    }

    this[from] = null
    this[to] = piece.apply { square = to }
    currentColor = currentColor.opposite()

    return MoveResult.Success(Move(BoardUtils.toCord(from), BoardUtils.toCord(to), piece))
  }

  fun promote(from: Int, to: Int, pieceName: String): PromotionResult {
    val pawn = this[from] as? Pawn ?: return PromotionResult.Error("Не пешка")

    if (!ruleService.isPromotion(pawn, to)) {
      return PromotionResult.Error("Некорректное превращение")
    }

    val newPiece = when (pieceName) {
      "Queen" -> Queen(pawn.color, to)
      "Rook" -> Rook(pawn.color, to)
      "Bishop" -> Bishop(pawn.color, to)
      "Knight" -> Knight(pawn.color, to)
      else -> return PromotionResult.Error("Неверная фигура")
    }

    this[from] = null
    this[to] = newPiece
    currentColor = currentColor.opposite()

    return PromotionResult.Success(
      Move(BoardUtils.toCord(from), BoardUtils.toCord(to), pawn, pieceName)
    )
  }

  fun getGameState(): GameState = ruleService.getGameState(this, currentColor)

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