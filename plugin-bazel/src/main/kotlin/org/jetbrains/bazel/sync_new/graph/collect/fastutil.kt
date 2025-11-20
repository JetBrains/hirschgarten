package org.jetbrains.bazel.sync_new.graph.collect

import it.unimi.dsi.fastutil.longs.LongList

inline fun LongList.forEachFast(crossinline func: (element: Long) -> Unit) {
  for (n in indices) {
    func(getLong(n))
  }
}
