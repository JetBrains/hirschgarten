package org.jetbrains.bazel.sync_new.flow.vfs_diff.starlark

import org.jetbrains.bazel.label.Label
import java.nio.file.Path

class StarlarkParsedFile(
  val path: Path,
  val loads: List<Label>
)
