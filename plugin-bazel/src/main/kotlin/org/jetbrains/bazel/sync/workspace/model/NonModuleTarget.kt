package org.jetbrains.bazel.sync.workspace.model

import org.jetbrains.bazel.commons.Tag
import org.jetbrains.bazel.label.Label
import java.nio.file.Path

data class NonModuleTarget(
  val label: Label,
  val tags: Set<Tag>,
  val baseDirectory: Path,
  val kindString: String,
  val generatorName: String?,
)
