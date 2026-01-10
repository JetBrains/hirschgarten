package org.jetbrains.bazel.sync_new.storage.hash

import com.dynatrace.hash4j.hashing.HashStream128
import com.dynatrace.hash4j.hashing.HashValue128
import com.dynatrace.hash4j.hashing.Hashing
import org.jetbrains.bazel.label.Label
import java.nio.file.Path
import kotlin.io.path.absolutePathString

fun hash(op: HashStream128.() -> Unit): HashValue128 {
  return Hashing.xxh3_128().hashStream().apply(op).get()
}

fun hash(path: Path): HashValue128 = hash { putString(path.absolutePathString()) }
fun hash(label: Label): HashValue128 = hash { putLabel(label) }
