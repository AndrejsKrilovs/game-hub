package krilovs.andrejs.chess.application.bot

import org.springframework.stereotype.Component

@Component
class TranspositionTable {

  private val table = HashMap<Long, TTEntry>(200_000)

  fun clear() {
    table.clear()
  }

  fun get(key: Long): TTEntry? = table[key]

  fun put(key: Long, entry: TTEntry) {
    table[key] = entry
  }
}