package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import com.intellij.util.containers.CollectionFactory
import org.h2.mvstore.MVMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

// global generation object table, value frames keep type information (`writeClassAndObject`),
// so a single table can hold objects of any type

internal const val REF_TABLE_WRITE_KEY: String = "snapshot.refTable.write"
internal const val REF_TABLE_READ_KEY: String = "snapshot.refTable.read"

internal class RefTableWriter {
  // keyed by value equality, equal objects share one id
  private val ids = ConcurrentHashMap<Any, Int>()
  private val nextId = AtomicInteger(1)

  fun idFor(value: Any): Int = ids.computeIfAbsent(value) { nextId.getAndIncrement() }

  // ascending by id, as required by MVMap.append
  fun sortedEntries(): List<Pair<Int, Any>> = ids.entries.map { it.value to it.key }.sortedBy { it.first }
}

internal class RefTableReader(
  private val refs: MVMap<Int, ValueFrame>,
  private val decode: (ValueFrame) -> Any,
) {
  private val cache = CollectionFactory.createConcurrentWeakValueMap<Int, Any>()

  fun get(id: Int): Any = cache.getOrPut(id) { decode(refs[id] ?: error("ref table has no entry for id $id")) }
}
