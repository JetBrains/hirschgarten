package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import org.h2.mvstore.MVMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

// TODO: right now only specialized serializers use string table,
//  we could add some type marker (e.g. PoolableString) so open types can use it, e.g., for compiler args

internal const val STRING_TABLE_WRITE_KEY: String = "snapshot.stringTable.write"
internal const val STRING_TABLE_READ_KEY: String = "snapshot.stringTable.read"

internal class StringTableWriter {
  private val ids = ConcurrentHashMap<String, Int>()
  private val nextId = AtomicInteger(1)

  fun idFor(value: String): Int = ids.computeIfAbsent(value) { nextId.getAndIncrement() }

  // ascending by id, as required by MVMap.append
  fun sortedEntries(): List<Pair<Int, String>> = ids.entries.map { it.value to it.key }.sortedBy { it.first }
}

internal class StringTableReader(private val strings: MVMap<Int, String>) {
  private val cache = ConcurrentHashMap<Int, String>()

  fun get(id: Int): String =
    cache.computeIfAbsent(id) { strings[it] ?: error("string table has no entry for id $it") }
}
