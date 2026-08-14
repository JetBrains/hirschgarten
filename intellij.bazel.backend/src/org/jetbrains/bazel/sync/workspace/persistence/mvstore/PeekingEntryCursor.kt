package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import org.h2.mvstore.MVMap

internal class PeekingEntryCursor<K, V>(map: MVMap<K, V>, private val keyToLong: (K) -> Long) {
  private val iterator = map.entries.iterator()
  private var peeked: Map.Entry<K, V>? = null

  private fun peek(): Map.Entry<K, V>? {
    if (peeked == null && iterator.hasNext()) {
      peeked = iterator.next()
    }
    return peeked
  }

  fun advanceTo(target: Long): V? {
    while (true) {
      val entry = peek() ?: return null
      val entryKey = keyToLong(entry.key)
      when {
        entryKey < target -> peeked = null
        entryKey == target -> {
          peeked = null
          return entry.value
        }

        else -> return null
      }
    }
  }
}
