package org.jetbrains.bazel.sync_new.storage.bsearch

fun interface BinarySearch<T> {
  fun search(items: Array<T>, key: T, size: Int, cached: Int): Int
}
