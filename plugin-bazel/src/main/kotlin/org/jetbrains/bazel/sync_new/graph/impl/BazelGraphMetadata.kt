package org.jetbrains.bazel.sync_new.graph.impl

import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged

@Tagged
data class BazelGraphMetadata(
  @field:Tag(1)
  var vertexIdCounter: Int = 1,

  @field:Tag(2)
  var edgeIdCounter: Int = 1
)
