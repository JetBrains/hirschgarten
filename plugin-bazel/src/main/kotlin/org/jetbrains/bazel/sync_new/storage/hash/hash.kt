package org.jetbrains.bazel.sync_new.storage.hash

import com.dynatrace.hash4j.hashing.HashStream128
import com.dynatrace.hash4j.hashing.HashValue128
import com.dynatrace.hash4j.hashing.Hashing
import org.jetbrains.bazel.sync_new.codec.Codec

inline fun hash(crossinline op: HashStream128.() -> Unit): HashValue128 {
  return Hashing.xxh3_128().hashStream().apply(op).get()
}

fun hash128Comparator(): Comparator<HashValue128> = Comparator { left, right ->
  when {
    left.mostSignificantBits > right.mostSignificantBits -> 1
    left.mostSignificantBits < right.mostSignificantBits -> -1
    left.leastSignificantBits > right.leastSignificantBits -> 1
    left.leastSignificantBits < right.leastSignificantBits -> -1
    else -> 0
  }
}
