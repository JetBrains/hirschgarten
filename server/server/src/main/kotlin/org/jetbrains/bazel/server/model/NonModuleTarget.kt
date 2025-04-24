package org.jetbrains.bazel.server.model

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.label.Label
import java.nio.file.Path

data class NonModuleTarget(
  val label: Label,
  val languages: Set<LanguageClass>,
  val tags: Set<Tag>,
  val baseDirectory: Path,
  val kindString: String,
)
