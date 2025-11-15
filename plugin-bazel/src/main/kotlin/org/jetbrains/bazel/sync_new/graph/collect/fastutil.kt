package org.jetbrains.bazel.sync_new.graph.collect

import it.unimi.dsi.fastutil.longs.LongList

@Suppress("ReplaceManualRangeWithIndicesCalls")
inline fun LongList.forEachFast(crossinline func: (element: Long) -> Unit) {
  for (n in 0 until size) {
    func(getLong(n))
  }
}
