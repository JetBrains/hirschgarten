package org.jetbrains.bazel.sync_new.flow.vfs_diff.starlark

import org.jetbrains.bazel.sync_new.codec.kryo.EnumTag
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTagged

@EnumTagged
enum class StarlarkFileKind {
  @EnumTag(1)
  BUILD,

  @EnumTag(2)
  WORKSPACE,

  @EnumTag(3)
  STARLARK
}
