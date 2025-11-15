package org.jetbrains.bazel.sync_new.graph.impl

import com.jetbrains.bazel.sync_new.proto.BazelTargetVertexProto
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.converterOf
import org.jetbrains.bazel.sync_new.graph.TargetVertex
import org.jetbrains.bazel.sync_new.graph.VertexId

class BazelTargetVertex(val proto: BazelTargetVertexProto) : TargetVertex {
  companion object {
    internal val converter = converterOf<BazelTargetVertexProto, BazelTargetVertex>(
      to = { BazelTargetVertex(it) },
      from = { it.proto}
    )
  }

  override val id: VertexId
    get() = proto.vertexId
  override val label: Label by lazy { Label.parse(proto.canonicalLabel) }
}
