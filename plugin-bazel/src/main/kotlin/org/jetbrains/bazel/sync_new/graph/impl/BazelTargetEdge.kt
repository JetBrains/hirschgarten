package org.jetbrains.bazel.sync_new.graph.impl

import com.jetbrains.bazel.sync_new.proto.BazelTargetEdgeProto
import org.jetbrains.bazel.sync_new.codec.converterOf
import org.jetbrains.bazel.sync_new.graph.EdgeId
import org.jetbrains.bazel.sync_new.graph.ID
import org.jetbrains.bazel.sync_new.graph.TargetEdge
import org.jetbrains.bazel.sync_new.graph.VertexId

class BazelTargetEdge(val proto: BazelTargetEdgeProto) : TargetEdge {
  companion object {
    internal val converter = converterOf<BazelTargetEdgeProto, BazelTargetEdge>(
      to = { BazelTargetEdge(it) },
      from = { it.proto }
    )
  }

  override val id: ID
    get() = proto.edgeId
  override val from: VertexId
    get() = proto.fromVertexId
  override val to: VertexId
    get() = proto.toVertexId
}
