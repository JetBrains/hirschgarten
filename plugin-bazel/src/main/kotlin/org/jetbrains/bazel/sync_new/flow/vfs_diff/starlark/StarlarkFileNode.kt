package org.jetbrains.bazel.sync_new.flow.vfs_diff.starlark

import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged
import java.nio.file.Path

@Tagged
data class StarlarkFileNode(
  @field:Tag(1)
  val workspacePath: Path,

  @field:Tag(2)
  val kind: StarlarkFileKind,
)
