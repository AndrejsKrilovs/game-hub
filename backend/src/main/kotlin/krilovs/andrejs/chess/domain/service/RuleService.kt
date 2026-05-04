package krilovs.andrejs.chess.domain.service

import krilovs.andrejs.chess.domain.model.Board
import krilovs.andrejs.chess.domain.model.GameState
import krilovs.andrejs.chess.domain.piece.Bishop
import krilovs.andrejs.chess.domain.model.Color
import krilovs.andrejs.chess.domain.piece.King
import krilovs.andrejs.chess.domain.piece.Knight
import krilovs.andrejs.chess.domain.piece.Pawn
import krilovs.andrejs.chess.domain.piece.Piece
import krilovs.andrejs.chess.domain.piece.Rook
import krilovs.andrejs.chess.utils.BoardUtils
import krilovs.andrejs.chess.domain.model.CastlingType
import org.springframework.stereotype.Component
import kotlin.math.abs

@Component
class RuleService {
  fun isSafeMove(board: Board, from: Int, to: Int): Boolean {
    val piece = board[from] ?: return false
    val captured = board[to]

    if (piece is King && abs(to - from) == 2) {
      if (isSquareUnderAttack(board, findKing(board, piece.color), piece.color.opposite())) return false

      // клетка, через которую проходит король при рокировках не должна быть под шахом
      val step = if (to > from) 1 else -1
      if (isSquareUnderAttack(board, from + step, piece.color.opposite())) return false
    }

    board[from] = null
    board[to] = piece
    val oldSquare = piece.square
    piece.square = to

    val inCheck = isSquareUnderAttack(board, findKing(board, piece.color), piece.color.opposite())

    board[from] = piece
    board[to] = captured
    piece.square = oldSquare

    return !inCheck
  }

  fun isSquareUnderAttack(board: Board, square: Int, byColor: Color): Boolean =
    board.getPieces()
      .asSequence()
      .filter { it.color == byColor }
      .any { piece -> piece.generateAttacks(board).contains(square) }

  fun getGameState(board: Board, currentTurn: Color): GameState {
    val kingSquare = findKing(board, currentTurn)

    val hasMoves = board.getPieces()
      .asSequence()
      .filter { it.color == currentTurn }
      .flatMap { piece ->
        piece.generateAvailableMoves(board).asSequence().map { to -> piece.square to to }
      }
      .any { (from, to) -> isSafeMove(board, from, to) }

    val inCheck = isSquareUnderAttack(board, kingSquare, currentTurn.opposite())

    return when {
      isInsufficientMaterial(board) -> GameState.DRAW
      !inCheck && !hasMoves -> GameState.STALEMATE
      inCheck && !hasMoves -> GameState.CHECKMATE
      inCheck -> GameState.CHECK
      else -> GameState.NORMAL
    }
  }

  fun isPromotion(piece: Piece, to: Int): Boolean =
    piece is Pawn && when (piece.color) {
      Color.WHITE -> to / 8 == 7
      Color.BLACK -> to / 8 == 0
    }

  fun getCastlingType(from: Int, to: Int): CastlingType =
    if (to - from == 2) CastlingType.SHORT
    else CastlingType.LONG

  fun getCastlingRightsToRemove(piece: Piece, to: Int, captured: Piece?): Set<Char> =
    buildSet {
      val rookRights = mapOf(
        0 to 'Q', 7 to 'K',
        56 to 'q', 63 to 'k'
      )

      if (piece is King) addAll(if (piece.color == Color.WHITE) "KQ".toList() else "kq".toList())
      if (piece is Rook) rookRights[piece.square]?.let(::add)
      if (captured is Rook) rookRights[to]?.let(::add)
    }

  fun isCastling(piece: Piece, from: Int, to: Int): Boolean =
    piece is King && abs(to - from) == 2

  private fun isInsufficientMaterial(board: Board): Boolean {
    val nonKings = board.getPieces().filterNot { it is King }

    // только короли
    if (nonKings.isEmpty()) return true

    // король + лёгкая фигура vs король
    if (nonKings.size == 1) {
      return nonKings[0] is Bishop || nonKings[0] is Knight
    }

    // K+B vs K+B (оба слона на одном цвете)
    if (nonKings.all { it is Bishop }) {
      val colors = nonKings.map { BoardUtils.squareColor(it.square) }
      return colors.toSet().size == 1
    }

    return false
  }

  private fun findKing(board: Board, color: Color): Int =
    board.getPieces().first { it is King && it.color == color }.square
}