package org.jetbrains.bazel.sync_new.graph.impl

import com.jetbrains.bazel.sync_new.proto.BazelTargetCompactProto
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.converterOf
import org.jetbrains.bazel.sync_new.graph.ID
import org.jetbrains.bazel.sync_new.graph.TargetCompact

class BazelTargetCompact(val proto: BazelTargetCompactProto) : TargetCompact {
  companion object {
    internal val converter = converterOf<BazelTargetCompactProto, BazelTargetCompact>(
      to = { BazelTargetCompact(it) },
      from = { it.proto }
    )
  }

  override val id: ID
    get() = proto.vertexId
  override val label: Label by lazy { Label.parse(proto.canonicalLabel) }
  override val isExecutable: Boolean
    get() = proto.isExecutable
}
