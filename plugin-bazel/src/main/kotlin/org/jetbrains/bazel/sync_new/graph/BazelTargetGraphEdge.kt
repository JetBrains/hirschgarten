package org.jetbrains.bazel.sync_new.graph

import com.jetbrains.bazel.sync_new.proto.BazelTargetDependency
import org.jetbrains.bazel.sync_new.codec.converterOf

data class BazelTargetGraphEdge(val inner: BazelTargetDependency) {
  companion object {
    internal val converter = converterOf<BazelTargetDependency, BazelTargetGraphEdge>(
      to = { BazelTargetGraphEdge(it) },
      from = { it.inner },
    )
  }
}
