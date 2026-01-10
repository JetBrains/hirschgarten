package org.jetbrains.bazel.sync_new.flow.vfs_diff.starlark

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.Bind
import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import org.jetbrains.bazel.sync_new.codec.kryo.ClassTag
import org.jetbrains.bazel.sync_new.codec.kryo.KryoNIOPathSerializer
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged
import java.nio.file.Path

@Tagged
@ClassTag(1521486402)
data class StarlarkFileNode(
  @field:Tag(1)
  @field:Bind(valueClass = Path::class, serializer = KryoNIOPathSerializer::class)
  val workspacePath: Path,

  @field:Tag(2)
  val kind: StarlarkFileKind,
)
