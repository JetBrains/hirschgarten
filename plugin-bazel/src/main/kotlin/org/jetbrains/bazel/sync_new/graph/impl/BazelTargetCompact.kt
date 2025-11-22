package org.jetbrains.bazel.sync_new.graph.impl

import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged
import org.jetbrains.bazel.sync_new.graph.ID
import org.jetbrains.bazel.sync_new.graph.TargetCompact

@Tagged
data class BazelTargetCompact(
  @field:Tag(1)
  override val vertexId: ID,

  @field:Tag(2)
  override val label: Label,

  @field:Tag(3)
  override val isExecutable: Boolean = false
) : TargetCompact
