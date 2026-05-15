package krilovs.andrejs.chess.application

import krilovs.andrejs.chess.application.bot.UndoMove
import krilovs.andrejs.chess.domain.model.*
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
  private val pieceFactory: PieceFactory,
  private val moveGenerator: MoveGeneratorService
) {
  private val board: Board = Board()

  lateinit var currentTurn: Color
  lateinit var castlingOption: String

  fun getBoard(): Board = board
  fun getPiece(square: Int): Piece? = board[square]
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

    val moves = moveGenerator
      .generateLegalMoves(board, piece, castlingOption)
      .map { Move(BoardUtils.toCord(square), BoardUtils.toCord(it), piece) }
      .toSet()

    return when {
      moves.isEmpty() -> AvailableMovesResult.Error("Нет доступных ходов")
      else -> AvailableMovesResult.Success(moves)
    }
  }

  fun makeMove(from: Int, to: Int): MoveResult {
    val piece = board[from] ?: return MoveResult.Error("Пустая клетка")
    val legalMoves = moveGenerator.generateLegalMoves(board, piece, castlingOption)
    if (to !in legalMoves) return MoveResult.Error("Некорректный ход")

    val baseMove = Move(BoardUtils.toCord(from), BoardUtils.toCord(to), piece)
    if (rules.promotion.isPromotion(piece, to)) {
      return MoveResult.Promotion(setOf("Queen", "Rook", "Bishop", "Knight"), baseMove)
    }

    val captured = board[to]
    val toRemove = rules.castling.getCastlingRightsToRemove(piece, to, captured)
    applyMove(from, to, piece)

    val castlingType = handleCastling(piece, from, to)
    currentTurn = currentTurn.opposite()
    castlingOption = castlingOption.filterNot { it in toRemove }.ifEmpty { "-" }

    return MoveResult.Success(castlingType?.let { baseMove.copy(castlingType = it) } ?: baseMove)
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

  fun getAllMoves(): Set<Move> = moveGenerator
    .getAllMoves(board, currentTurn, castlingOption)
    .mapNotNull { (from, to) ->
      val piece = board[from] ?: return@mapNotNull null
      Move(BoardUtils.toCord(from), BoardUtils.toCord(to), piece)
    }
    .toSet()

  fun getGameState(): GameState = rules.gameState.getGameState(board, currentTurn)

  private fun applyCastlingRookMove(color: Color, type: CastlingType) {
    val rank = if (color == Color.WHITE) 0 else 7
    val (fromFile, toFile) = if (type == CastlingType.SHORT) 7 to 5 else 0 to 3

    val rookFrom = rank * 8 + fromFile
    val rookTo = rank * 8 + toFile

    board[rookTo] = board[rookFrom]
    board[rookFrom] = null
    board[rookTo]?.square = rookTo
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

  fun makeMoveInternal(from: Int, to: Int): UndoMove {
    val piece = board[from]!!
    val captured = board[to]

    val undo = UndoMove(
      from = from,
      to = to,
      movedPiece = piece,
      capturedPiece = captured,
      previousCastling = castlingOption,
      previousTurn = currentTurn
    )


    board[from] = null
    board[to] = piece.apply { square = to }
    currentTurn = currentTurn.opposite()
    return undo
  }

  fun undoMove(undo: UndoMove) {
    val piece = undo.movedPiece

    board[undo.from] = piece.apply { square = undo.from }
    board[undo.to] = undo.capturedPiece

    castlingOption = undo.previousCastling
    currentTurn = undo.previousTurn
  }
}