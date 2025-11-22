package org.jetbrains.bazel.sync_new.flow.diff

import com.dynatrace.hash4j.hashing.HashValue128
import org.jetbrains.bazel.label.Label

data class TargetHash(
  val target: Label,
  val hash: HashValue128
)
