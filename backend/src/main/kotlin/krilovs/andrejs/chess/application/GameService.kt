package krilovs.andrejs.chess.application

import krilovs.andrejs.chess.domain.model.*
import krilovs.andrejs.chess.domain.piece.King
import krilovs.andrejs.chess.domain.piece.Pawn
import krilovs.andrejs.chess.domain.piece.Piece
import krilovs.andrejs.chess.domain.service.RuleFacade
import krilovs.andrejs.chess.dto.AvailableMovesResult
import krilovs.andrejs.chess.dto.MoveResult
import krilovs.andrejs.chess.dto.PromotionResult
import krilovs.andrejs.chess.utils.BoardUtils
import org.springframework.stereotype.Component

@Component
class GameService(
  private val rules: RuleFacade,
  private val pieceFactory: PieceFactory
) {
  private val board: Board = Board()

  lateinit var currentTurn: Color
  lateinit var castlingOption: String

  fun getPieces(): Set<Piece> = board.getPieces()

  fun loadFromFEN(fen: String) {
    board.clear()

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
          board[square] = pieceFactory.create(char.lowercaseChar(), color, square)
          file++
        }
      }
    }

    castlingOption = castlingPart
    currentTurn = if (turnPart == "w") Color.WHITE else Color.BLACK
  }

  fun generateMovesForSquare(square: Int): AvailableMovesResult {
    val piece = board[square] ?: return AvailableMovesResult.Error("Пустая клетка")

    if (piece.color != currentTurn) {
      return AvailableMovesResult.Error("Фигура другого цвета")
    }

    val moves = piece.generateAvailableMoves(board).toMutableSet()
    if (piece is King) {
      moves.addAll(rules.castling.getCastlingMoves(board, piece, castlingOption))
    }

    val availableMoves = moves
      .filter { to -> rules.moveSafety.isSafeMove(board, square, to) }
      .map { Move(BoardUtils.toCord(square), BoardUtils.toCord(it), piece) }
      .toSet()

    return when {
      availableMoves.isEmpty() -> AvailableMovesResult.Error("Нет доступных ходов")
      else -> AvailableMovesResult.Success(availableMoves)
    }
  }

  fun makeMove(from: Int, to: Int): MoveResult {
    val piece = board[from] ?: return MoveResult.Error("Пустая клетка")
    val baseMove = Move(BoardUtils.toCord(from), BoardUtils.toCord(to), piece)

    if (!isValidMove(piece, from, to)) {
      return MoveResult.Error("Некорректный ход")
    }

    if (rules.promotion.isPromotion(piece, to)) {
      return MoveResult.Promotion(
        availablePieces = setOf("Queen", "Rook", "Bishop", "Knight"),
        move = baseMove
      )
    }

    val captured = board[to]
    val toRemove = rules.castling.getCastlingRightsToRemove(piece, to, captured)

    applyMove(from, to, piece)

    val castlingType = handleCastling(piece, from, to)

    currentTurn = currentTurn.opposite()
    castlingOption = castlingOption.filterNot { it in toRemove }.ifEmpty { "-" }

    return MoveResult.Success(
      castlingType?.let { baseMove.copy(castlingType = it) } ?: baseMove
    )
  }

  fun promote(from: Int, to: Int, pieceName: String): PromotionResult {
    val pawn = board[from] as? Pawn ?: return PromotionResult.Error("Не пешка")

    if (!rules.promotion.isPromotion(pawn, to)) {
      return PromotionResult.Error("Некорректное превращение")
    }

    val newPiece = pieceFactory.create(pieceName.first().lowercaseChar(), pawn.color, to)

    board[from] = null
    board[to] = newPiece
    currentTurn = currentTurn.opposite()

    return PromotionResult.Success(
      Move(BoardUtils.toCord(from), BoardUtils.toCord(to), pawn, pieceName)
    )
  }

  fun getGameState(): GameState = rules.gameState.getGameState(board, currentTurn)

  private fun applyCastlingRookMove(color: Color, type: CastlingType) {
    val rank = if (color == Color.WHITE) 0 else 7

    val (rookFromFile, rookToFile) = when (type) {
      CastlingType.SHORT -> 7 to 5
      CastlingType.LONG -> 0 to 3
    }

    val rookFrom = rank * 8 + rookFromFile
    val rookTo = rank * 8 + rookToFile

    board[rookTo] = board[rookFrom]
    board[rookFrom] = null
    board[rookTo]?.square = rookTo
  }

  private fun isValidMove(piece: Piece, from: Int, to: Int): Boolean {
    val moves = piece.generateAvailableMoves(board).toMutableSet()
    if (piece is King) {
      moves.addAll(rules.castling.getCastlingMoves(board, piece, castlingOption))
    }

    return to in moves && rules.moveSafety.isSafeMove(board, from, to)
  }

  private fun applyMove(from: Int, to: Int, piece: Piece) {
    board[from] = null
    board[to] = piece.apply { square = to }
  }

  private fun handleCastling(piece: Piece, from: Int, to: Int): CastlingType? {
    if (!rules.castling.isCastling(piece, from, to)) return null

    val type = rules.castling.getCastlingType(from, to)
    applyCastlingRookMove(piece.color, type)
    return type
  }
}