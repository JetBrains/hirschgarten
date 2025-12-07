package org.jetbrains.bazel.sync_new.flow.vfs_diff.starlark

import org.jetbrains.bazel.sync_new.codec.kryo.ClassTag
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTag
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTagged

@EnumTagged
@ClassTag(1695504627)
enum class StarlarkFileKind {
  @EnumTag(1)
  BUILD,

  @EnumTag(2)
  WORKSPACE,

  @EnumTag(3)
  STARLARK
}
