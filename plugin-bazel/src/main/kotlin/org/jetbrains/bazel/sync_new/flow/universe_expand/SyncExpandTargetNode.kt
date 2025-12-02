package org.jetbrains.bazel.sync_new.flow.universe_expand

import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged

@Tagged
data class SyncExpandTargetNode(
  @field:Tag(1)
  val label: Label
)
