package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class Int2ObjectBiMap<T>(
  val forward: Int2ObjectMap<T> = Int2ObjectOpenHashMap(),
  val reverse: Object2IntMap<T> = Object2IntOpenHashMap()
) {
  operator fun set(key: Int, value: T) {
    forward.put(key, value)
    reverse.put(value, key)
  }

  operator fun get(key: Int): T? = forward.get(key)
  fun getReverseOrDefault(key: T, default: Int): Int = reverse.getOrDefault(key, default)
}
