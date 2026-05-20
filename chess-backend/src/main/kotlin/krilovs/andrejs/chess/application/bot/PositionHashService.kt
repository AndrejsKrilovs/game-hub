package krilovs.andrejs.chess.application.bot

import krilovs.andrejs.chess.application.GameService
import krilovs.andrejs.chess.domain.piece.Bishop
import krilovs.andrejs.chess.domain.piece.King
import krilovs.andrejs.chess.domain.piece.Knight
import krilovs.andrejs.chess.domain.piece.Pawn
import krilovs.andrejs.chess.domain.piece.Piece
import krilovs.andrejs.chess.domain.piece.Queen
import krilovs.andrejs.chess.domain.piece.Rook
import org.springframework.stereotype.Component

@Component
class PositionHashService(private val game: GameService) {

  fun key(): Long {
    var hash = HASH_OFFSET

    for (square in 0..63) {
      val piece = game.getPiece(square) ?: continue

      hash = mix(hash, square)
      hash = mix(hash, pieceCode(piece))
      hash = mix(hash, piece.color.ordinal)
    }

    hash = mix(hash, game.currentTurn.ordinal)
    for (char in game.castlingOption) {
      hash = mix(hash, char.code)
    }

    return hash
  }

  private fun mix(hash: Long, value: Int): Long = (hash xor value.toLong()) * HASH_PRIME

  private fun pieceCode(piece: Piece): Int =
    when (piece) {
      is Pawn -> 1
      is Knight -> 2
      is Bishop -> 3
      is Rook -> 4
      is Queen -> 5
      is King -> 6
      else -> 0
    }

  companion object {
    private const val HASH_OFFSET = 1469598103934665603L
    private const val HASH_PRIME = 1099511628211L
  }
}