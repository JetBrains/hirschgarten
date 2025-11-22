package org.jetbrains.bazel.sync_new.graph.impl

import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged
import org.jetbrains.bazel.sync_new.graph.ID
import org.jetbrains.bazel.sync_new.graph.TargetEdge

@Tagged
data class BazelTargetEdge(
  @field:Tag(1)
  override val edgeId: ID,

  @field:Tag(2)
  override val from: ID,

  @field:Tag(3)
  override val to: ID
) : TargetEdge
