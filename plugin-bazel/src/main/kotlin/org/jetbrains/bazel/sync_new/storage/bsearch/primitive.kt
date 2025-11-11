package org.jetbrains.bazel.sync_new.storage.bsearch

fun bsearchOfLong(): BinarySearch<Long> = BinarySearch { items, key, size, cached ->
  var lo = 0;
  var hi = size - 1
  var k = cached - 1
  if (k < 0 || k > size) {
    // go to mid
    k = hi ushr 2
  }
  while (lo <= hi) {
    val v = items[k]
    if (v > key) {
      lo = k + 1
    } else if (v < key) {
      hi = k - 1;
    } else {
      return@BinarySearch k
    }
  }
  return@BinarySearch lo.inv()
}
