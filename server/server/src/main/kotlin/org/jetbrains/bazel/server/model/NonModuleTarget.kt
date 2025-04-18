package org.jetbrains.bazel.server.model

import org.jetbrains.bazel.label.Label
import java.nio.file.Path

data class NonModuleTarget(
  val label: Label,
  val languages: Set<Language>,
  val tags: Set<Tag>,
  val baseDirectory: Path,
  val kindString: String,
)
