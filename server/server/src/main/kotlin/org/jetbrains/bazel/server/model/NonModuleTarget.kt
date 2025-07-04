package org.jetbrains.bazel.server.model

import org.jetbrains.bazel.label.CanonicalLabel
import java.nio.file.Path

data class NonModuleTarget(
  val label: CanonicalLabel,
  val tags: Set<Tag>,
  val baseDirectory: Path,
  val kindString: String,
)
