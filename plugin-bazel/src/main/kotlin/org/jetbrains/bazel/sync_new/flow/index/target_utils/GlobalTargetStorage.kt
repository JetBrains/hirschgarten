package org.jetbrains.bazel.sync_new.flow.index.target_utils

import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.kryo.ClassTag
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged

@Tagged
@ClassTag(1982387901)
internal data class GlobalTargetStorage(
  @field:TaggedFieldSerializer.Tag(1)
  val allTargets: List<Label> = listOf(),

  @field:TaggedFieldSerializer.Tag(2)
  val allTargetsLabels: List<String> = listOf(),

  @field:TaggedFieldSerializer.Tag(3)
  val allExecutableTargetsLabels: List<String> = listOf(),
)
