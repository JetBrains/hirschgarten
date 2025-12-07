package org.jetbrains.bazel.sync_new.flow.universe_expand

import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.kryo.ClassTag
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged

@Tagged
@ClassTag(1978921280)
data class SyncExpandTargetNode(
  @field:Tag(1)
  val label: Label
)
