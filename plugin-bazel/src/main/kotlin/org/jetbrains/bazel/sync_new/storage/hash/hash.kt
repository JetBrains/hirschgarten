package org.jetbrains.bazel.sync_new.storage.hash

import com.dynatrace.hash4j.hashing.HashStream128
import com.dynatrace.hash4j.hashing.HashValue128
import com.dynatrace.hash4j.hashing.Hashing
import org.jetbrains.bazel.label.Label
import java.nio.file.Path
import kotlin.io.path.absolutePathString

val hash128Comparator: Comparator<HashValue128> = Comparator { lhs, rhs ->
  when {
    lhs.mostSignificantBits < rhs.mostSignificantBits -> -1
    lhs.mostSignificantBits > rhs.mostSignificantBits -> 1
    lhs.leastSignificantBits < rhs.leastSignificantBits -> -1
    lhs.leastSignificantBits > rhs.leastSignificantBits -> 1
    else -> 0
  }
}

fun hash(op: HashStream128.() -> Unit): HashValue128 {
  return Hashing.xxh3_128().hashStream().apply(op).get()
}

fun hash(path: Path): HashValue128 = hash { putString(path.absolutePathString()) }
fun hash(label: Label): HashValue128 = hash { putLabel(label) }

inline fun <T> HashStream128.putNullable(obj: T?, crossinline hash: HashStream128.(obj: T) -> Unit) {
  when (obj) {
    null -> {
      putByte(0)
    }
    else -> {
      putByte(1)
      hash(obj)
    }
  }
}

fun HashStream128.putHash128(hash: HashValue128) {
  putLong(hash.mostSignificantBits)
  putLong(hash.leastSignificantBits)
}

fun <T> HashStream128.putOrdered(iterable: Iterable<T>, fn: HashStream128.(obj: T) -> Unit) {
  for (element in iterable) {
    val hash = hash { fn(element) }
    putHash128(hash)
  }
}

fun <T> HashStream128.putUnordered(iterable: Iterable<T>, fn: HashStream128.(obj: T) -> Unit) {
  val hashes = iterable.map { hash { fn(it) } }
    .sortedWith(hash128Comparator)
  putOrdered(hashes) { putHash128(it) }
}
