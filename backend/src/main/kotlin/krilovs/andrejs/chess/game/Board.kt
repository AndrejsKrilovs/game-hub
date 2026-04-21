package krilovs.andrejs.chess.game

import krilovs.andrejs.chess.piece.Bishop
import krilovs.andrejs.chess.piece.King
import krilovs.andrejs.chess.piece.Knight
import krilovs.andrejs.chess.piece.Pawn
import krilovs.andrejs.chess.piece.Piece
import krilovs.andrejs.chess.piece.Queen
import krilovs.andrejs.chess.piece.Rook

class Board {
  var currentTurn = Color.WHITE
  var whiteKingMoved = false
  var blackKingMoved = false
  var whiteRookAMoved = false
  var whiteRookHMoved = false
  var blackRookAMoved = false
  var blackRookHMoved = false

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

    currentTurn = if (turnPart == "w") Color.WHITE else Color.BLACK

    whiteKingMoved = true
    blackKingMoved = true
    whiteRookAMoved = true
    whiteRookHMoved = true
    blackRookAMoved = true
    blackRookHMoved = true

    castlingPart.takeIf { it != "-" }?.let {
      if ('K' in it) { whiteKingMoved = false; whiteRookHMoved = false }
      if ('Q' in it) { whiteKingMoved = false; whiteRookAMoved = false }
      if ('k' in it) { blackKingMoved = false; blackRookHMoved = false }
      if ('q' in it) { blackKingMoved = false; blackRookAMoved = false }
    }
  }

  fun toFEN(): String = buildString {
    for (rank in 7 downTo 0) {
      var empty = 0

      for (file in 0..7) {
        val piece = this@Board[rank * 8 + file]
        if (piece == null) empty++
        else {
          if (empty > 0) append(empty).also { empty = 0 }
          append(piece.toFenChar())
        }
      }

      if (empty > 0) append(empty)
      if (rank > 0) append("/")
    }

    append(" ")
    append(if (currentTurn == Color.WHITE) "w" else "b")
    append(" ")

    append(buildString {
      if (!whiteKingMoved && !whiteRookHMoved) append("K")
      if (!whiteKingMoved && !whiteRookAMoved) append("Q")
      if (!blackKingMoved && !blackRookHMoved) append("k")
      if (!blackKingMoved && !blackRookAMoved) append("q")
    }.ifEmpty { "-" })

    append(" - 0 1")
  }

  fun makeMove(move: Move) {
    val piece = move.piece
    this[move.from] = null

    val newPiece = move.promotion
      ?.let { createPiece(it, piece.color, move.to) }
      ?: piece

    this[move.to] = newPiece
    newPiece.square = move.to

    if (move.isCastling && piece is King) {
      when (move.to) {
        6  -> moveRook(7, 5)
        2  -> moveRook(0, 3)
        62 -> moveRook(63, 61)
        58 -> moveRook(56, 59)
      }
    }

    currentTurn = currentTurn.opposite()
  }

  fun unmakeMove(move: Move) {
    currentTurn = currentTurn.opposite()

    if (move.isCastling && move.piece is King) {
      when (move.to) {
        6  -> moveRook(5, 7)
        2  -> moveRook(3, 0)
        62 -> moveRook(61, 63)
        58 -> moveRook(59, 56)
      }
    }

    this[move.from] = move.piece
    this[move.to] = move.captured

    move.piece.square = move.from
    move.captured?.square = move.to
  }

  fun generateMoves(): List<Move> =
    pieces
      .asSequence()
      .filter { it.color == currentTurn }
      .flatMap { piece ->
        buildList { piece.generateMoves(this@Board, this) }
      }
      .toList()

  fun generateMovesForSquare(square: Int): List<Move> {
    val piece = this[square] ?: return emptyList()
    if (piece.color != currentTurn) return emptyList()

    val pseudo = buildList { piece.generateMoves(this@Board, this) }
    val rules = GameRules(this, AttackService(this))
    return pseudo.filter(rules::isMoveSafe)
  }

  fun castlingData(color: Color): CastlingData =
    when (color) {
      Color.WHITE -> CastlingData(
        kingStart = 4,
        emptyShort = listOf(5, 6),
        emptyLong = listOf(1, 2, 3),
        kingMoved = whiteKingMoved,
        rookAMoved = whiteRookAMoved,
        rookHMoved = whiteRookHMoved
      )

      Color.BLACK -> CastlingData(
        kingStart = 60,
        emptyShort = listOf(61, 62),
        emptyLong = listOf(57, 58, 59),
        kingMoved = blackKingMoved,
        rookAMoved = blackRookAMoved,
        rookHMoved = blackRookHMoved
      )
    }

  fun file(sq: Int) = sq % 8
  fun rank(sq: Int) = sq / 8
  fun isInside(sq: Int) = sq in 0..63

  private fun moveRook(from: Int, to: Int) {
    val rook = this[from] ?: return
    this[to] = rook
    this[from] = null
    rook.square = to
  }

  private fun createPiece(type: Char, color: Color, square: Int): Piece =
    when (type) {
      'p' -> Pawn(color, square)
      'r' -> Rook(color, square)
      'n' -> Knight(color, square)
      'b' -> Bishop(color, square)
      'q' -> Queen(color, square)
      'k' -> King(color, square)
      else -> error("Unknown piece: $type")
    }

  private fun Piece.toFenChar(): Char {
    val c = when (this) {
      is Pawn -> 'p'
      is Rook -> 'r'
      is Knight -> 'n'
      is Bishop -> 'b'
      is Queen -> 'q'
      is King -> 'k'
      else -> error("Unknown piece")
    }
    return if (color == Color.WHITE) c.uppercaseChar() else c
  }
}