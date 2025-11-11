package org.jetbrains.bazel.sync_new.graph

import com.jetbrains.bazel.sync_new.proto.BazelTargetNode
import org.jetbrains.bazel.sync_new.codec.converterOf

data class BazelTargetGraphVertex(val inner: BazelTargetNode) {
  companion object {
    internal val converter = converterOf<BazelTargetNode, BazelTargetGraphVertex>(
      to = { BazelTargetGraphVertex(it) },
      from = { it.inner },
    )
  }
}
